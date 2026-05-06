package org.codenbug.auth.app;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.Map;
import java.util.Optional;

import org.codenbug.auth.domain.SecurityUser;
import org.codenbug.auth.domain.SecurityUserId;
import org.codenbug.auth.domain.SecurityUserRepository;
import org.codenbug.auth.domain.UserId;
import org.codenbug.auth.ui.CreateOperationalAccountRequest;
import org.codenbug.auth.ui.LoginRequest;
import org.codenbug.auth.ui.RegisterRequest;
import org.codenbug.common.Role;
import org.codenbug.common.TokenInfo;
import org.codenbug.common.Util;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ValidationException;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService 테스트")
class AuthServiceTest {

    @Mock
    private SecurityUserRepository securityUserRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private ApplicationEventPublisher publisher;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private ProviderFactory factory;

    @Mock
    private UserRegistrationValidator userRegistrationValidator;

    @Mock
    private HttpServletRequest request;

    @InjectMocks
    private AuthService authService;

    private final String testKey = "testSecretKeyForJwtTokenGenerationThatIsLongEnough";

    @BeforeEach
    void setUp() {
        // JWT secret key 설정
        ReflectionTestUtils.setField(authService, "key", testKey);
    }

    @Nested
    @DisplayName("register 메서드 테스트")
    class RegisterTest {

        @Test
        @DisplayName("성공: 일반 회원가입은 USER 역할 계정을 생성하고 사용자 생성 이벤트를 발행한다")
        void 일반_회원가입_성공시_USER_역할_계정_생성_이벤트_발행() {
            RegisterRequest registerRequest = registerRequest();
            SecurityUserId securityUserId = new SecurityUserId("security-user-1");

            when(securityUserRepository.findSecurityUserByEmail("new-user@example.com"))
                    .thenReturn(Optional.empty());
            when(passwordEncoder.encode("password123")).thenReturn("encoded-password");
            when(securityUserRepository.save(any(SecurityUser.class))).thenReturn(securityUserId);

            SecurityUserId result = authService.register(registerRequest);

            assertEquals(securityUserId, result);
            verify(userRegistrationValidator).validateRegisterInputs(registerRequest);
            verify(securityUserRepository).save(argThat(user ->
                    "new-user@example.com".equals(user.getEmail())
                            && "encoded-password".equals(user.getPassword())
                            && Role.USER.name().equals(user.getRole())));
            verify(publisher).publishEvent(any(Object.class));
        }

        @Test
        @DisplayName("실패: 이메일이 이미 존재하면 계정을 생성하지 않는다")
        void 이메일_중복시_회원가입_실패() {
            RegisterRequest registerRequest = registerRequest();
            SecurityUser existingUser = SecurityUser.createUserAccount("new-user@example.com",
                    "encoded-password");

            when(securityUserRepository.findSecurityUserByEmail("new-user@example.com"))
                    .thenReturn(Optional.of(existingUser));

            assertThrows(ValidationException.class, () -> authService.register(registerRequest));

            verify(userRegistrationValidator).validateRegisterInputs(registerRequest);
            verify(securityUserRepository, never()).save(any(SecurityUser.class));
            verify(publisher, never()).publishEvent(any(Object.class));
        }

        private RegisterRequest registerRequest() {
            RegisterRequest request = new RegisterRequest();
            ReflectionTestUtils.setField(request, "email", "new-user@example.com");
            ReflectionTestUtils.setField(request, "password", "password123");
            ReflectionTestUtils.setField(request, "name", "New User");
            ReflectionTestUtils.setField(request, "age", 20);
            ReflectionTestUtils.setField(request, "sex", "M");
            ReflectionTestUtils.setField(request, "phoneNum", "010-0000-0000");
            ReflectionTestUtils.setField(request, "location", "Seoul");
            return request;
        }
    }

    @Nested
    @DisplayName("createOperationalAccount 메서드 테스트")
    class CreateOperationalAccountTest {

        @Test
        @DisplayName("성공: 관리자는 MANAGER 계정을 생성할 수 있다")
        void 관리자가_MANAGER_계정_생성_성공() {
            CreateOperationalAccountRequest request = operationalAccountRequest(Role.MANAGER);
            SecurityUserId securityUserId = new SecurityUserId("manager-security-user-1");

            when(securityUserRepository.findSecurityUserByEmail("manager@example.com"))
                    .thenReturn(Optional.empty());
            when(passwordEncoder.encode("password123")).thenReturn("encoded-password");
            when(securityUserRepository.save(any(SecurityUser.class))).thenReturn(securityUserId);

            SecurityUserId result = authService.createOperationalAccount(request);

            assertEquals(securityUserId, result);
            verify(securityUserRepository).save(argThat(user ->
                    "manager@example.com".equals(user.getEmail())
                            && "encoded-password".equals(user.getPassword())
                            && Role.MANAGER.name().equals(user.getRole())));
        }

        @Test
        @DisplayName("성공: 관리자는 USER 계정을 생성할 수 있다")
        void 관리자가_USER_계정_생성_성공() {
            CreateOperationalAccountRequest request = operationalAccountRequest(Role.USER);
            SecurityUserId securityUserId = new SecurityUserId("user-security-user-1");

            when(securityUserRepository.findSecurityUserByEmail("manager@example.com"))
                    .thenReturn(Optional.empty());
            when(passwordEncoder.encode("password123")).thenReturn("encoded-password");
            when(securityUserRepository.save(any(SecurityUser.class))).thenReturn(securityUserId);

            SecurityUserId result = authService.createOperationalAccount(request);

            assertEquals(securityUserId, result);
            verify(securityUserRepository).save(argThat(user -> Role.USER.name().equals(user.getRole())));
        }

        @Test
        @DisplayName("실패: 중복 이메일이면 운영 계정을 생성하지 않는다")
        void 중복_이메일로_운영_계정_생성_실패() {
            CreateOperationalAccountRequest request = operationalAccountRequest(Role.ADMIN);
            SecurityUser existingUser = SecurityUser.createOperationalAccount("manager@example.com",
                    "encoded-password", Role.MANAGER);

            when(securityUserRepository.findSecurityUserByEmail("manager@example.com"))
                    .thenReturn(Optional.of(existingUser));

            assertThrows(ValidationException.class,
                    () -> authService.createOperationalAccount(request));

            verify(securityUserRepository, never()).save(any(SecurityUser.class));
        }

        private CreateOperationalAccountRequest operationalAccountRequest(Role role) {
            CreateOperationalAccountRequest request = new CreateOperationalAccountRequest();
            ReflectionTestUtils.setField(request, "email", "manager@example.com");
            ReflectionTestUtils.setField(request, "password", "password123");
            ReflectionTestUtils.setField(request, "role", role);
            return request;
        }
    }

    @Nested
    @DisplayName("account status and admin login 테스트")
    class AccountStatusAndAdminLoginTest {

        @Test
        @DisplayName("실패: 정지 사용자는 로그인할 수 없다")
        void 정지_사용자_로그인_실패() {
            LoginRequest loginRequest = loginRequest();
            SecurityUser user = SecurityUser.createUserAccount("blocked@example.com", "encoded-password");
            user.updateUserId(new UserId("user-1"));
            user.suspend();

            when(securityUserRepository.findSecurityUserByEmail("blocked@example.com"))
                    .thenReturn(Optional.of(user));

            assertThrows(AccessDeniedException.class, () -> authService.loginEmail(loginRequest));
            verify(passwordEncoder, never()).matches(anyString(), anyString());
        }

        @Test
        @DisplayName("성공: 관리자 로그인 성공은 실패 횟수를 초기화한다")
        void 관리자_로그인_성공시_실패_횟수_초기화() {
            SecurityUser admin = SecurityUser.createOperationalAccount("admin@example.com",
                    "encoded-password", Role.ADMIN);
            admin.recordAdminLoginFailure();

            when(securityUserRepository.findSecurityUserByEmail("admin@example.com"))
                    .thenReturn(Optional.of(admin));
            when(passwordEncoder.matches("password123", "encoded-password")).thenReturn(true);

            SecurityUser result = authService.authenticateBackofficeAdmin("admin@example.com", "password123");

            assertSame(admin, result);
            assertEquals(0, admin.getLoginAttemptCount());
            verify(securityUserRepository).save(admin);
        }

        @Test
        @DisplayName("실패: 관리자 로그인 실패 3회는 계정을 잠근다")
        void 관리자_로그인_실패_3회시_계정_잠금() {
            SecurityUser admin = SecurityUser.createOperationalAccount("admin@example.com",
                    "encoded-password", Role.ADMIN);

            when(securityUserRepository.findSecurityUserByEmail("admin@example.com"))
                    .thenReturn(Optional.of(admin));
            when(passwordEncoder.matches("wrong", "encoded-password")).thenReturn(false);

            assertThrows(AccessDeniedException.class,
                    () -> authService.authenticateBackofficeAdmin("admin@example.com", "wrong"));
            assertThrows(AccessDeniedException.class,
                    () -> authService.authenticateBackofficeAdmin("admin@example.com", "wrong"));
            assertThrows(AccessDeniedException.class,
                    () -> authService.authenticateBackofficeAdmin("admin@example.com", "wrong"));

            assertTrue(admin.isAccountLocked());
            assertEquals(3, admin.getLoginAttemptCount());
        }

        @Test
        @DisplayName("성공: 관리자는 다른 관리자 계정을 논리 삭제할 수 있다")
        void 다른_관리자_계정_삭제_허용() {
            SecurityUser targetAdmin = SecurityUser.createOperationalAccount("other-admin@example.com",
                    "encoded-password", Role.ADMIN);

            when(securityUserRepository.findSecurityUser(any(SecurityUserId.class)))
                    .thenReturn(Optional.of(targetAdmin));

            authService.logicalDelete("admin-security-user-1", "target-security-user-1");

            assertEquals(org.codenbug.auth.domain.AccountStatus.DELETED, targetAdmin.getAccountStatus());
            verify(securityUserRepository).save(targetAdmin);
        }

        @Test
        @DisplayName("실패: 삭제된 사용자는 정지 또는 정지 해제를 재조작할 수 없다")
        void 삭제된_계정은_정지_또는_정지_해제_불가() {
            SecurityUser user = SecurityUser.createUserAccount("deleted@example.com", "encoded-password");
            user.logicalDelete();

            when(securityUserRepository.findSecurityUser(any(SecurityUserId.class)))
                    .thenReturn(Optional.of(user));

            assertThrows(IllegalStateException.class, () -> authService.suspend("target-security-user-1"));
            assertThrows(IllegalStateException.class, () -> authService.unsuspend("target-security-user-1"));
        }

        private LoginRequest loginRequest() {
            LoginRequest request = new LoginRequest();
            ReflectionTestUtils.setField(request, "email", "blocked@example.com");
            ReflectionTestUtils.setField(request, "password", "password123");
            return request;
        }
    }

    @Nested
    @DisplayName("refreshTokens 메서드 테스트")
    class RefreshTokensTest {

        @Test
        @DisplayName("성공: 정상적인 헤더가 제공된 경우 새로운 토큰을 발급한다")
        void 토큰_재발급_유효한_헤더로_성공() {
            // Given
            String userId = "user123";
            String email = "test@example.com";
            String role = "USER";

            when(request.getHeader("User-Id")).thenReturn(userId);
            when(request.getHeader("Email")).thenReturn(email);
            when(request.getHeader("Role")).thenReturn(role);

            // When
            TokenInfo result = authService.refreshTokens(request);

            // Then
            assertNotNull(result);
            assertNotNull(result.getAccessToken());
            assertNotNull(result.getRefreshToken());
            assertNotNull(result.getAccessToken().getRawValue());
            assertNotNull(result.getRefreshToken().getValue());
        }

        @Test
        @DisplayName("성공: MANAGER 역할로 토큰 재발급이 가능하다")
        void 토큰_재발급_MANAGER_역할로_성공() {
            // Given
            String userId = "manager123";
            String email = "manager@example.com";
            String role = "MANAGER";

            when(request.getHeader("User-Id")).thenReturn(userId);
            when(request.getHeader("Email")).thenReturn(email);
            when(request.getHeader("Role")).thenReturn(role);

            // When
            TokenInfo result = authService.refreshTokens(request);

            // Then
            assertNotNull(result);
            assertNotNull(result.getAccessToken());
            assertNotNull(result.getRefreshToken());
        }

        @Test
        @DisplayName("성공: ADMIN 역할로 토큰 재발급이 가능하다")
        void 토큰_재발급_ADMIN_역할로_성공() {
            // Given
            String userId = "admin123";
            String email = "admin@example.com";
            String role = "ADMIN";

            when(request.getHeader("User-Id")).thenReturn(userId);
            when(request.getHeader("Email")).thenReturn(email);
            when(request.getHeader("Role")).thenReturn(role);

            // When
            TokenInfo result = authService.refreshTokens(request);

            // Then
            assertNotNull(result);
            assertNotNull(result.getAccessToken());
            assertNotNull(result.getRefreshToken());
        }

        @Test
        @DisplayName("실패: User-Id 헤더가 누락된 경우 예외가 발생한다")
        void 토큰_재발급_UserId_헤더_누락시_실패() {
            // Given
            when(request.getHeader("User-Id")).thenReturn(null);
            when(request.getHeader("Email")).thenReturn("test@example.com");
            when(request.getHeader("Role")).thenReturn("USER");

            // When & Then
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                    () -> authService.refreshTokens(request));
            assertEquals("Missing required headers from gateway", exception.getMessage());
        }

        @Test
        @DisplayName("실패: Email 헤더가 누락된 경우 예외가 발생한다")
        void 토큰_재발급_Email_헤더_누락시_실패() {
            // Given
            when(request.getHeader("User-Id")).thenReturn("user123");
            when(request.getHeader("Email")).thenReturn(null);
            when(request.getHeader("Role")).thenReturn("USER");

            // When & Then
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                    () -> authService.refreshTokens(request));
            assertEquals("Missing required headers from gateway", exception.getMessage());
        }

        @Test
        @DisplayName("실패: Role 헤더가 누락된 경우 예외가 발생한다")
        void 토큰_재발급_Role_헤더_누락시_실패() {
            // Given
            when(request.getHeader("User-Id")).thenReturn("user123");
            when(request.getHeader("Email")).thenReturn("test@example.com");
            when(request.getHeader("Role")).thenReturn(null);

            // When & Then
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                    () -> authService.refreshTokens(request));
            assertEquals("Missing required headers from gateway", exception.getMessage());
        }

        @Test
        @DisplayName("실패: 모든 헤더가 누락된 경우 예외가 발생한다")
        void 토큰_재발급_모든_헤더_누락시_실패() {
            // Given
            when(request.getHeader("User-Id")).thenReturn(null);
            when(request.getHeader("Email")).thenReturn(null);
            when(request.getHeader("Role")).thenReturn(null);

            // When & Then
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                    () -> authService.refreshTokens(request));
            assertEquals("Missing required headers from gateway", exception.getMessage());
        }

        @Test
        @DisplayName("실패: 잘못된 Role 값이 전달된 경우 예외가 발생한다")
        void 토큰_재발급_잘못된_Role_값_실패() {
            // Given
            when(request.getHeader("User-Id")).thenReturn("user123");
            when(request.getHeader("Email")).thenReturn("test@example.com");
            when(request.getHeader("Role")).thenReturn("INVALID_ROLE");

            // When & Then
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                    () -> authService.refreshTokens(request));
            assertTrue(exception.getMessage().contains("No enum constant"));
        }

        @Test
        @DisplayName("실패: 빈 문자열 헤더가 전달된 경우 예외가 발생한다")
        void 토큰_재발급_빈_문자열_헤더_실패() {
            // Given
            when(request.getHeader("User-Id")).thenReturn("");
            when(request.getHeader("Email")).thenReturn("");
            when(request.getHeader("Role")).thenReturn("");

            // When & Then
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                    () -> authService.refreshTokens(request));
            // 빈 문자열의 경우 Role.valueOf("")에서 예외가 발생함
            assertTrue(exception.getMessage().contains("No enum constant"));
        }

        @Test
        @DisplayName("성공: 공백이 포함된 헤더 값도 처리된다")
        void 토큰_재발급_공백_포함된_헤더_성공() {
            // Given
            String userId = " user123 ";
            String email = " test@example.com ";
            String role = "USER";

            when(request.getHeader("User-Id")).thenReturn(userId);
            when(request.getHeader("Email")).thenReturn(email);
            when(request.getHeader("Role")).thenReturn(role);

            // When
            TokenInfo result = authService.refreshTokens(request);

            // Then
            assertNotNull(result);
            assertNotNull(result.getAccessToken());
            assertNotNull(result.getRefreshToken());
        }

        @Test
        @DisplayName("성공: 특수 문자가 포함된 사용자 ID로도 토큰이 발급된다")
        void 토큰_재발급_특수문자_포함_UserId_성공() {
            // Given
            String userId = "user-123_test@domain";
            String email = "test@example.com";
            String role = "USER";

            when(request.getHeader("User-Id")).thenReturn(userId);
            when(request.getHeader("Email")).thenReturn(email);
            when(request.getHeader("Role")).thenReturn(role);

            // When
            TokenInfo result = authService.refreshTokens(request);

            // Then
            assertNotNull(result);
            assertNotNull(result.getAccessToken());
            assertNotNull(result.getRefreshToken());
        }

        @Test
        @DisplayName("성공: 긴 이메일 주소로도 토큰이 발급된다")
        void 토큰_재발급_긴_이메일_성공() {
            // Given
            String userId = "user123";
            String email = "very.long.email.address.for.testing@very.long.domain.example.com";
            String role = "USER";

            when(request.getHeader("User-Id")).thenReturn(userId);
            when(request.getHeader("Email")).thenReturn(email);
            when(request.getHeader("Role")).thenReturn(role);

            // When
            TokenInfo result = authService.refreshTokens(request);

            // Then
            assertNotNull(result);
            assertNotNull(result.getAccessToken());
            assertNotNull(result.getRefreshToken());
        }
    }
}
