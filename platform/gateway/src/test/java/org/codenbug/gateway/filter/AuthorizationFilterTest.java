package org.codenbug.gateway.filter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;

import org.codenbug.common.TokenInfo;
import org.codenbug.common.Util;
import org.codenbug.common.exception.BlacklistedRefreshTokenException;
import org.codenbug.gateway.config.WhitelistProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpCookie;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ServerWebExchange;

import com.fasterxml.jackson.databind.ObjectMapper;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class AuthorizationFilterTest {

    private static final String JWT_SECRET = "testSecretKeyForJwtTokenGenerationThatIsLongEnough";

    @Mock
    private RedisRefreshTokenBlackList refreshTokenStorage;

    @Mock
    private GatewayFilterChain chain;

    private AuthorizationFilter authorizationFilter;
    private WhitelistProperties whitelistProperties;

    @BeforeEach
    void setUp() {
        whitelistProperties = new WhitelistProperties();
        authorizationFilter = new AuthorizationFilter(whitelistProperties, refreshTokenStorage,
            new ObjectMapper());
        ReflectionTestUtils.setField(authorizationFilter, "jwtSecret", JWT_SECRET);
    }

    @Test
    void 화이트리스트_퍼블릭_엔드포인트_토큰_없이_허용() {
        whitelistProperties.getUrls()
            .add(new WhitelistProperties.Urls("GET", "/api/v1/events/{id}"));
        MockServerWebExchange exchange = MockServerWebExchange.from(
            org.springframework.mock.http.server.reactive.MockServerHttpRequest
                .get("/api/v1/events/event-1")
                .build());
        GatewayFilter gatewayFilter = authorizationFilter.apply(new AuthorizationFilter.Config());

        when(chain.filter(any(ServerWebExchange.class))).thenReturn(Mono.empty());

        StepVerifier.create(gatewayFilter.filter(exchange, chain))
            .verifyComplete();

        verify(refreshTokenStorage, never()).checkBlackList(any());
        verify(chain).filter(exchange);
    }

    @Test
    void 보호_엔드포인트_인증_요구() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
            org.springframework.mock.http.server.reactive.MockServerHttpRequest
                .post("/api/v1/purchases")
                .build());
        GatewayFilter gatewayFilter = authorizationFilter.apply(new AuthorizationFilter.Config());

        StepVerifier.create(gatewayFilter.filter(exchange, chain))
            .verifyComplete();

        assertEquals(HttpStatus.UNAUTHORIZED, exchange.getResponse().getStatusCode());
        StepVerifier.create(exchange.getResponse().getBodyAsString())
            .expectNextMatches(body -> body.contains("사용자 오류") && !body.contains("refreshToken is null"))
            .verifyComplete();
        verify(chain, never()).filter(any(ServerWebExchange.class));
    }

    @Test
    void 기본_블랙리스트_체크_건너뛰고_체인_계속() {
        TokenInfo tokenInfo = generateTokenInfo();
        MockServerWebExchange exchange = exchangeForPolling(tokenInfo);
        GatewayFilter gatewayFilter = authorizationFilter.apply(new AuthorizationFilter.Config());

        when(chain.filter(any(ServerWebExchange.class))).thenReturn(Mono.empty());

        StepVerifier.create(gatewayFilter.filter(exchange, chain))
            .verifyComplete();

        verify(refreshTokenStorage, never()).checkBlackList(any());
        verify(chain).filter(any(ServerWebExchange.class));
    }

    @Test
    void 리프레시_토큰_블랙리스트_인증_실패() {
        TokenInfo tokenInfo = generateTokenInfo();
        MockServerWebExchange exchange = exchangeForPolling(tokenInfo);
        ReflectionTestUtils.setField(authorizationFilter,
            "checkRefreshBlacklistOnEachRequest", true);
        GatewayFilter gatewayFilter = authorizationFilter.apply(new AuthorizationFilter.Config());

        when(refreshTokenStorage.checkBlackList(any()))
            .thenReturn(Mono.error(new BlacklistedRefreshTokenException("Refresh token is blacklisted.")));

        StepVerifier.create(gatewayFilter.filter(exchange, chain))
            .verifyComplete();

        assertEquals(HttpStatus.UNAUTHORIZED, exchange.getResponse().getStatusCode());
        StepVerifier.create(exchange.getResponse().getBodyAsString())
            .expectNextMatches(body -> body.contains("사용자 오류")
                && !body.contains("Refresh token is blacklisted"))
            .verifyComplete();
        verify(refreshTokenStorage).checkBlackList(any());
        verify(chain, never()).filter(any(ServerWebExchange.class));
    }

    private TokenInfo generateTokenInfo() {
        return Util.generateTokens(
            Map.of("userId", "user-1", "role", "USER", "email", "user-1@ticketon.site"),
            Util.Key.convertSecretKey(JWT_SECRET));
    }

    private MockServerWebExchange exchangeForPolling(TokenInfo tokenInfo) {
        return MockServerWebExchange.from(org.springframework.mock.http.server.reactive.MockServerHttpRequest
            .get("/api/v1/broker/polling/events/event-1/current")
            .header("Authorization",
                tokenInfo.getAccessToken().getType() + " " + tokenInfo.getAccessToken().getRawValue())
            .cookie(new HttpCookie("refreshToken", tokenInfo.getRefreshToken().getValue()))
            .build());
    }
}
