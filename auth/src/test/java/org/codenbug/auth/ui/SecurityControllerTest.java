package org.codenbug.auth.ui;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import org.codenbug.auth.app.AuthService;
import org.codenbug.auth.app.OAuthService;
import org.codenbug.auth.domain.RefreshTokenBlackList;
import org.codenbug.auth.domain.SecurityUserId;
import org.codenbug.common.AccessToken;
import org.codenbug.common.RefreshToken;
import org.codenbug.common.Role;
import org.codenbug.common.RsData;
import org.codenbug.common.TokenInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;

@ExtendWith(MockitoExtension.class)
@DisplayName("SecurityController 테스트")
class SecurityControllerTest {

    @Mock
    private AuthService authService;

    @Mock
    private OAuthService oAuthService;

    @Mock
    private RefreshTokenBlackList blackList;

    private SecurityController securityController;
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;

    @BeforeEach
    void setUp() {
        securityController = new SecurityController(authService, oAuthService, blackList);
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
    }

    @Test
    @DisplayName("성공: 운영 권한 계정 생성 요청은 AuthService로 위임하고 201을 반환한다")
    void 운영_계정_생성_성공() {
        CreateOperationalAccountRequest accountRequest = new CreateOperationalAccountRequest();
        ReflectionTestUtils.setField(accountRequest, "email", "manager@example.com");
        ReflectionTestUtils.setField(accountRequest, "password", "password123");
        ReflectionTestUtils.setField(accountRequest, "role", Role.MANAGER);
        SecurityUserId securityUserId = new SecurityUserId("manager-security-user-1");

        when(authService.createOperationalAccount(accountRequest)).thenReturn(securityUserId);

        ResponseEntity<RsData<SecurityUserId>> result =
                securityController.createOperationalAccount(accountRequest);

        assertEquals(HttpStatus.CREATED, result.getStatusCode());
        assertEquals(securityUserId, result.getBody().getData());
        verify(authService).createOperationalAccount(accountRequest);
    }

    @Test
    @DisplayName("성공: 로그아웃은 refresh token 쿠키를 만료시키고 blacklist에 등록한다")
    void 로그아웃_성공시_리프레시_토큰_블랙리스트_등록() {
        Cookie refreshToken = new Cookie("refreshToken", "refresh-token-value");
        request.setCookies(refreshToken);
        request.addHeader("User-Id", "user-1");

        ResponseEntity<RsData<Void>> result = securityController.logout(request, response);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(0, response.getCookie("refreshToken").getMaxAge());
        verify(blackList).add(eq("user-1"), any(Cookie.class));
    }

    @Nested
    @DisplayName("refreshTokens 메서드 테스트")
    class RefreshTokensEndpointTest {

        @Test
        @DisplayName("성공: 정상적인 토큰 재발급 요청 시 200 OK와 새로운 토큰을 반환한다")
        void 토큰_재발급_성공시_새로운_토큰_반환() {
            // Given
            AccessToken accessToken = new AccessToken("new-access-token-value", "Bearer");
            RefreshToken refreshToken = new RefreshToken("new-refresh-token-value");
            TokenInfo tokenInfo = new TokenInfo(accessToken, refreshToken);

            when(authService.refreshTokens(any(HttpServletRequest.class))).thenReturn(tokenInfo);

            // When
            ResponseEntity<RsData<String>> result =
                    securityController.refreshTokens(request, response);

            // Then
            assertEquals(HttpStatus.OK, result.getStatusCode());
            assertEquals("200", result.getBody().getCode());
            assertEquals("토큰 재발급 성공", result.getBody().getMessage());
            assertEquals("new-access-token-value", result.getBody().getData());

            // 응답 헤더 확인
            assertEquals("new-access-token-value", response.getHeader("Authorization"));

            // 쿠키 확인
            Cookie[] cookies = response.getCookies();
            assertNotNull(cookies);
            assertEquals(1, cookies.length);
            assertEquals("refreshToken", cookies[0].getName());
            assertEquals("new-refresh-token-value", cookies[0].getValue());
            assertEquals(604800, cookies[0].getMaxAge()); // 7 days
            assertEquals("/", cookies[0].getPath());
            assertTrue(cookies[0].getSecure());
            assertFalse(cookies[0].isHttpOnly());

            verify(authService, times(1)).refreshTokens(any(HttpServletRequest.class));
        }

        @Test
        @DisplayName("실패: AuthService에서 IllegalArgumentException 발생 시 401 Unauthorized를 반환한다")
        void 토큰_재발급_IllegalArgumentException_실패시_401_반환() {
            // Given
            when(authService.refreshTokens(any(HttpServletRequest.class))).thenThrow(
                    new IllegalArgumentException("Missing required headers from gateway"));

            // When
            ResponseEntity<RsData<String>> result =
                    securityController.refreshTokens(request, response);

            // Then
            assertEquals(HttpStatus.UNAUTHORIZED, result.getStatusCode());
            assertEquals("401", result.getBody().getCode());
            assertEquals("토큰 재발급 실패: Missing required headers from gateway",
                    result.getBody().getMessage());
            assertNull(result.getBody().getData());

            verify(authService, times(1)).refreshTokens(any(HttpServletRequest.class));
        }

        @Test
        @DisplayName("실패: AuthService에서 RuntimeException 발생 시 401 Unauthorized를 반환한다")
        void 토큰_재발급_RuntimeException_실패시_401_반환() {
            // Given
            when(authService.refreshTokens(any(HttpServletRequest.class)))
                    .thenThrow(new RuntimeException("Token validation failed"));

            // When
            ResponseEntity<RsData<String>> result =
                    securityController.refreshTokens(request, response);

            // Then
            assertEquals(HttpStatus.UNAUTHORIZED, result.getStatusCode());
            assertEquals("401", result.getBody().getCode());
            assertEquals("토큰 재발급 실패: Token validation failed", result.getBody().getMessage());
            assertNull(result.getBody().getData());

            verify(authService, times(1)).refreshTokens(any(HttpServletRequest.class));
        }

        @Test
        @DisplayName("실패: AuthService에서 일반적인 Exception 발생 시 401 Unauthorized를 반환한다")
        void 토큰_재발급_일반_예외_실패시_401_반환() {
            // Given
            when(authService.refreshTokens(any(HttpServletRequest.class)))
                    .thenThrow(new RuntimeException("Unexpected error")); // Exception을
                                                                          // RuntimeException으로 변경

            // When
            ResponseEntity<RsData<String>> result =
                    securityController.refreshTokens(request, response);

            // Then
            assertEquals(HttpStatus.UNAUTHORIZED, result.getStatusCode());
            assertEquals("401", result.getBody().getCode());
            assertEquals("토큰 재발급 실패: Unexpected error", result.getBody().getMessage());
            assertNull(result.getBody().getData());

            verify(authService, times(1)).refreshTokens(any(HttpServletRequest.class));
        }

        @Test
        @DisplayName("성공: JWT 타입이 다른 경우에도 정상적으로 처리된다")
        void 토큰_재발급_다른_토큰_타입으로_성공() {
            // Given
            AccessToken accessToken = new AccessToken("jwt-access-token-value", "JWT");
            RefreshToken refreshToken = new RefreshToken("jwt-refresh-token-value");
            TokenInfo tokenInfo = new TokenInfo(accessToken, refreshToken);

            when(authService.refreshTokens(any(HttpServletRequest.class))).thenReturn(tokenInfo);

            // When
            ResponseEntity<RsData<String>> result =
                    securityController.refreshTokens(request, response);

            // Then
            assertEquals(HttpStatus.OK, result.getStatusCode());
            assertEquals("200", result.getBody().getCode());
            assertEquals("토큰 재발급 성공", result.getBody().getMessage());
            assertEquals("jwt-access-token-value", result.getBody().getData());
            assertEquals("jwt-access-token-value", response.getHeader("Authorization"));

            Cookie[] cookies = response.getCookies();
            assertNotNull(cookies);
            assertEquals("jwt-refresh-token-value", cookies[0].getValue());

            verify(authService, times(1)).refreshTokens(any(HttpServletRequest.class));
        }

        @Test
        @DisplayName("성공: 긴 토큰 값도 정상적으로 처리된다")
        void 토큰_재발급_긴_토큰_값으로_성공() {
            // Given
            String longAccessTokenValue = "very-long-access-token-value-".repeat(10) + "end";
            String longRefreshTokenValue = "very-long-refresh-token-value-".repeat(10) + "end";

            AccessToken accessToken = new AccessToken(longAccessTokenValue, "Bearer");
            RefreshToken refreshToken = new RefreshToken(longRefreshTokenValue);
            TokenInfo tokenInfo = new TokenInfo(accessToken, refreshToken);

            when(authService.refreshTokens(any(HttpServletRequest.class))).thenReturn(tokenInfo);

            // When
            ResponseEntity<RsData<String>> result =
                    securityController.refreshTokens(request, response);

            // Then
            assertEquals(HttpStatus.OK, result.getStatusCode());
            assertEquals("200", result.getBody().getCode());
            assertEquals("토큰 재발급 성공", result.getBody().getMessage());
            assertEquals(longAccessTokenValue, result.getBody().getData());
            assertEquals(longAccessTokenValue, response.getHeader("Authorization"));

            Cookie[] cookies = response.getCookies();
            assertNotNull(cookies);
            assertEquals(longRefreshTokenValue, cookies[0].getValue());

            verify(authService, times(1)).refreshTokens(any(HttpServletRequest.class));
        }

        @Test
        @DisplayName("실패: AuthService가 null을 반환하는 경우 NullPointerException이 발생하고 401을 반환한다")
        void 토큰_재발급_AuthService_null_반환_실패시_401() {
            // Given
            when(authService.refreshTokens(any(HttpServletRequest.class))).thenReturn(null);

            // When
            ResponseEntity<RsData<String>> result =
                    securityController.refreshTokens(request, response);

            // Then
            assertEquals(HttpStatus.UNAUTHORIZED, result.getStatusCode());
            assertEquals("401", result.getBody().getCode());
            assertTrue(result.getBody().getMessage().contains("NullPointerException")
                    || result.getBody().getMessage().contains("null"));
            assertNull(result.getBody().getData());

            verify(authService, times(1)).refreshTokens(any(HttpServletRequest.class));
        }

        @Test
        @DisplayName("성공: 다양한 시나리오에서 AuthService 호출이 정상적으로 이루어진다")
        void 토큰_재발급_AuthService_호출_성공() {
            // Given
            AccessToken accessToken = new AccessToken("access-token-value", "Bearer");
            RefreshToken refreshToken = new RefreshToken("refresh-token-value");
            TokenInfo tokenInfo = new TokenInfo(accessToken, refreshToken);

            when(authService.refreshTokens(any(HttpServletRequest.class))).thenReturn(tokenInfo);

            // When
            ResponseEntity<RsData<String>> result =
                    securityController.refreshTokens(request, response);

            // Then
            assertEquals(HttpStatus.OK, result.getStatusCode());
            assertEquals("200", result.getBody().getCode());
            assertEquals("토큰 재발급 성공", result.getBody().getMessage());
            assertEquals("access-token-value", result.getBody().getData());

            verify(authService, times(1)).refreshTokens(any(HttpServletRequest.class));
        }
    }
}
