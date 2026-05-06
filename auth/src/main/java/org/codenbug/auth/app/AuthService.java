package org.codenbug.auth.app;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

import org.codenbug.auth.domain.SecurityUser;
import org.codenbug.auth.domain.SecurityUserId;
import org.codenbug.auth.domain.SecurityUserRepository;
import org.codenbug.auth.domain.SocialProvider;
import org.codenbug.auth.global.SocialLoginType;
import org.codenbug.auth.ui.CreateOperationalAccountRequest;
import org.codenbug.auth.ui.LoginRequest;
import org.codenbug.auth.ui.RegisterRequest;
import org.codenbug.common.Role;
import org.codenbug.common.TokenInfo;
import org.codenbug.common.Util;
import org.codenbug.message.SecurityUserRegisteredEvent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.ValidationException;

@Service
public class AuthService {

  private final ObjectMapper objectMapper;
  private SecurityUserRepository securityUserRepository;
  private PasswordEncoder passwordEncoder;
  private final ProviderFactory factory;
  @Value("${custom.jwt.secret}")
  private String key;
  private final ApplicationEventPublisher publisher;
  private final UserRegistrationValidator userRegistrationValidator;

  public AuthService(SecurityUserRepository securityUserRepository, PasswordEncoder passwordEncoder,
      ApplicationEventPublisher publisher, ObjectMapper objectMapper, ProviderFactory factory,
      UserRegistrationValidator userRegistrationValidator) {
    this.securityUserRepository = securityUserRepository;
    this.passwordEncoder = passwordEncoder;
    this.publisher = publisher;
    this.objectMapper = objectMapper;
    this.factory = factory;
    this.userRegistrationValidator = userRegistrationValidator;
  }

  @Transactional
  public TokenInfo loginEmail(LoginRequest loginRequest) {
    String email = loginRequest.getEmail();
    String password = loginRequest.getPassword();
    SecurityUser user = securityUserRepository.findSecurityUserByEmail(email)
        .orElseThrow(() -> new EntityNotFoundException("Email or password is wrong."));

    user.ensureCanAuthenticate();
    user.match(password, passwordEncoder);
    user.resetLoginFailures();

    TokenInfo tokenInfo = Util.generateTokens(Map.of("userId", user.tokenSubject(), "role",
        Role.valueOf(user.getRole()), "email", user.getEmail()), Util.Key.convertSecretKey(key));
    return tokenInfo;
  }

  // 이메일 회원가입
  @Transactional
  public SecurityUserId register(RegisterRequest request) {
    userRegistrationValidator.validateRegisterInputs(request);
    securityUserRepository.findSecurityUserByEmail(request.getEmail())
        .ifPresent(existing -> {
          throw new ValidationException("Email already exists.");
        });

    SecurityUserId saved = securityUserRepository
        .save(SecurityUser.createUserAccount(request.getEmail(),
            passwordEncoder.encode(request.getPassword())));
    publisher.publishEvent(new SecurityUserRegisteredEvent(saved.getValue(), request.getName(),
        request.getAge(), request.getSex(), request.getPhoneNum(), request.getLocation()));
    return saved;
  }

  @Transactional
  public SecurityUserId createOperationalAccount(CreateOperationalAccountRequest request) {
    return createAccount(request.getEmail(), request.getPassword(), request.getRole());
  }

  @Transactional
  public SecurityUserId createAccount(String email, String rawPassword, Role role) {
    if (role == null) {
      throw new ValidationException("Role is required.");
    }
    securityUserRepository.findSecurityUserByEmail(email)
        .ifPresent(existing -> {
          throw new ValidationException("Email already exists.");
        });

    return securityUserRepository.save(SecurityUser.createOperationalAccount(
        email,
        passwordEncoder.encode(rawPassword),
        role));
  }

  @Transactional
  public SecurityUser authenticateBackofficeAdmin(String email, String password) {
    SecurityUser user = securityUserRepository.findSecurityUserByEmail(email)
        .orElseThrow(() -> new AccessDeniedException("Backoffice credentials are invalid."));
    if (!user.hasRole(Role.ADMIN)) {
      throw new AccessDeniedException("Admin role is required.");
    }
    user.ensureCanAuthenticate();
    try {
      user.match(password, passwordEncoder);
    } catch (AccessDeniedException e) {
      user.recordAdminLoginFailure();
      securityUserRepository.save(user);
      if (user.isAccountLocked()) {
        throw new AccessDeniedException("Admin account is locked.");
      }
      throw e;
    }
    user.resetLoginFailures();
    securityUserRepository.save(user);
    return user;
  }

  public List<AdminAccountView> findAdminAccountViews() {
    return securityUserRepository.findAllSecurityUsers().stream()
        .sorted(Comparator.comparing(SecurityUser::getEmail))
        .map(AdminAccountView::from)
        .toList();
  }

  @Transactional
  public void promoteToManager(String targetSecurityUserId) {
    SecurityUser user = findAccount(targetSecurityUserId);
    user.promoteToManager();
    securityUserRepository.save(user);
  }

  @Transactional
  public void suspend(String targetSecurityUserId) {
    SecurityUser user = findAccount(targetSecurityUserId);
    user.suspend();
    securityUserRepository.save(user);
  }

  @Transactional
  public void unsuspend(String targetSecurityUserId) {
    SecurityUser user = findAccount(targetSecurityUserId);
    user.unsuspend();
    securityUserRepository.save(user);
  }

  @Transactional
  public void logicalDelete(String adminSecurityUserId, String targetSecurityUserId) {
    if (adminSecurityUserId != null && adminSecurityUserId.equals(targetSecurityUserId)) {
      throw new AccessDeniedException("Admin cannot delete own account.");
    }
    SecurityUser user = findAccount(targetSecurityUserId);
    user.logicalDelete();
    securityUserRepository.save(user);
  }

  private SecurityUser findAccount(String securityUserId) {
    return securityUserRepository.findSecurityUser(new SecurityUserId(securityUserId))
        .orElseThrow(() -> new EntityNotFoundException("Account not found."));
  }

  public String request(SocialLoginType socialLoginType) {
    SocialProvider provider = factory.getProvider(socialLoginType.name().toUpperCase());
    return provider.getOauthLoginUri();
  }

  public TokenInfo refreshTokens(jakarta.servlet.http.HttpServletRequest request) {
    String userId = request.getHeader("User-Id");
    String email = request.getHeader("Email");
    String role = request.getHeader("Role");

    if (userId == null || email == null || role == null) {
      throw new IllegalArgumentException("Missing required headers from gateway");
    }

    TokenInfo tokenInfo =
        Util.generateTokens(Map.of("userId", userId, "role", Role.valueOf(role), "email", email),
            Util.Key.convertSecretKey(key));

    return tokenInfo;
  }

}
