package org.codenbug.auth.app;

import java.util.Map;

import org.codenbug.auth.domain.SecurityUser;
import org.codenbug.auth.domain.SecurityUserId;
import org.codenbug.auth.domain.SecurityUserRepository;
import org.codenbug.auth.domain.SocialInfo;
import org.codenbug.auth.domain.SocialProvider;
import org.codenbug.auth.global.SocialLoginType;
import org.codenbug.auth.ui.RegisterRequest;
import org.codenbug.common.Role;
import org.codenbug.common.TokenInfo;
import org.codenbug.common.Util;
import org.codenbug.auth.ui.LoginRequest;
import org.codenbug.message.SecurityUserRegisteredEvent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.persistence.EntityNotFoundException;

@Service
public class AuthService {

	private final ObjectMapper objectMapper;
	private SecurityUserRepository securityUserRepository;
	private PasswordEncoder passwordEncoder;
	private final ProviderFactory factory;
	@Value("${custom.jwt.secret}")
	private String key;
	private final ApplicationEventPublisher publisher;

	public AuthService(SecurityUserRepository securityUserRepository, PasswordEncoder passwordEncoder,
		ApplicationEventPublisher publisher, ObjectMapper objectMapper, ProviderFactory factory) {
		this.securityUserRepository = securityUserRepository;
		this.passwordEncoder = passwordEncoder;
		this.publisher = publisher;
		this.objectMapper = objectMapper;
		this.factory = factory;
	}

	public TokenInfo loginEmail(LoginRequest loginRequest) {
		String email = loginRequest.getEmail();
		String password = loginRequest.getPassword();
		SecurityUser user = securityUserRepository.findSecurityUserByEmail(email)
			.orElseThrow(() -> new EntityNotFoundException("Email or password is wrong."));

		user.match(password, passwordEncoder);

		TokenInfo tokenInfo = Util.generateTokens(
			Map.of("userId", user.getUserId().getValue(), "role", Role.valueOf(user.getRole()), "email",
				user.getEmail()),
			Util.Key.convertSecretKey(key));
		return tokenInfo;
	}

	// 이메일 회원가입
	@Transactional
	public SecurityUserId register(RegisterRequest request) {
		SecurityUserId saved = securityUserRepository.save(new SecurityUser(new SocialInfo(null, null, false),
			request.getEmail(), passwordEncoder.encode(request.getPassword()), Role.USER.toString()));
		publisher.publishEvent(
			new SecurityUserRegisteredEvent(saved.getValue(), request.getName(), request.getAge(), request.getSex(),
				request.getPhoneNum(), request.getLocation()));
		return saved;
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
		
		TokenInfo tokenInfo = Util.generateTokens(
			Map.of("userId", userId, "role", Role.valueOf(role), "email", email),
			Util.Key.convertSecretKey(key));
			
		return tokenInfo;
	}

}
