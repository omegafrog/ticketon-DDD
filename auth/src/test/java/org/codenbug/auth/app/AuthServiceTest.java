package org.codenbug.auth.app;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.Map;

import org.codenbug.auth.domain.SecurityUserRepository;
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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.http.HttpServletRequest;

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
    @DisplayName("refreshTokens 메서드 테스트")
    class RefreshTokensTest {

        @Test
        @DisplayName("성공: 정상적인 헤더가 제공된 경우 새로운 토큰을 발급한다")
        void refreshTokens_Success_WithValidHeaders() {
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
        void refreshTokens_Success_WithManagerRole() {
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
        void refreshTokens_Success_WithAdminRole() {
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
        void refreshTokens_Fail_MissingUserIdHeader() {
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
        void refreshTokens_Fail_MissingEmailHeader() {
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
        void refreshTokens_Fail_MissingRoleHeader() {
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
        void refreshTokens_Fail_AllHeadersMissing() {
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
        void refreshTokens_Fail_InvalidRole() {
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
        void refreshTokens_Fail_EmptyStringHeaders() {
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
        void refreshTokens_Success_WithSpacesInHeaders() {
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
        void refreshTokens_Success_WithSpecialCharactersInUserId() {
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
        void refreshTokens_Success_WithLongEmail() {
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
