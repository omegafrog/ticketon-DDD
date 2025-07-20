package org.codenbug.gateway.filter;

import java.util.List;

import org.codenbug.common.AccessToken;
import org.codenbug.common.RefreshToken;
import org.codenbug.common.RsData;
import org.codenbug.common.TokenInfo;
import org.codenbug.common.Util;
import org.codenbug.common.exception.ExpiredJwtException;
import org.codenbug.common.exception.JwtException;
import org.codenbug.gateway.config.WhitelistProperties;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpCookie;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.server.ServerWebExchange;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Component
@Slf4j
public class AuthorizationFilter extends AbstractGatewayFilterFactory<AuthorizationFilter.Config> {
	@Value("${custom.jwt.secret}")
	private String jwtSecret;

	private final WhitelistProperties whitelistProperties;

	private final RedisRefreshTokenBlackList refreshTokenStorage;
	private final ObjectMapper objectMapper;

	public AuthorizationFilter(WhitelistProperties whitelistProperties, RedisRefreshTokenBlackList refreshTokenStorage, ObjectMapper objectMapper) {
		super(Config.class);
		this.whitelistProperties = whitelistProperties;
		this.refreshTokenStorage = refreshTokenStorage;
		this.objectMapper = objectMapper;
	}

	/**
	 * Get authorization token from request and set custom headers.
	 * Headers are {@code User-Id}, {@code Email}, {@code Role}.
	 * If the access token is expired and the refresh token is valid, refresh access token.
	 * @param config include Configuration information like bypass url
	 *
	 */
	@Override
	public GatewayFilter apply(Config config) {
		return (exchange, chain) -> {
			ServerHttpRequest request = exchange.getRequest();
			ServerHttpResponse response = exchange.getResponse();

			AccessToken accessToken;
			RefreshToken refreshToken = null;

			if (checkWhiteList(config, exchange, chain, request))
				return chain.filter(exchange);

			// check token validation
			try {
				accessToken = getAccessToken(request);
				refreshToken = getRefreshToken(request);

				refreshTokenStorage.checkBlackList(refreshToken);

				Util.validate(accessToken.getRawValue(), Util.Key.convertSecretKey(jwtSecret));
				Util.validate(refreshToken.getValue(), Util.Key.convertSecretKey(jwtSecret));
				refreshTokenStorage.checkBlackList(refreshToken);
			} catch (ExpiredJwtException e) {
				log.debug("access token is expired");
				TokenInfo tokenInfo = refreshAccessToken(refreshToken, e.getCause());
				accessToken = tokenInfo.getAccessToken();
				refreshToken = tokenInfo.getRefreshToken();

			} catch (JwtException  e) {
				return errorResponse(e, response);
			} catch (RuntimeException e){
				response.setStatusCode(HttpStatus.UNAUTHORIZED);
				response.getHeaders().add(HttpHeaders.CONTENT_TYPE, "application/json");
				try {
					return response.writeWith(Mono.just(response.bufferFactory().wrap(
						objectMapper.writeValueAsBytes(new RsData<Void>("401", e.getMessage(), null)))));
				} catch (JsonProcessingException ex) {
					throw new RuntimeException(ex);
				}
			}

			accessToken.decode(jwtSecret);
			ServerHttpRequest mutatedRequest = request.mutate()
				.header("Authorization", accessToken.getType() + " " + accessToken.getRawValue())
				.header(HttpHeaders.SET_COOKIE, setCookieHeader("refreshToken", refreshToken.getValue(),
					60 * 60 * 24 * 7, "/", false, false, "lat"))
				.header("User-Id", accessToken.getUserId())
				.header("Role", accessToken.getRole())
				.header("Email", accessToken.getEmail())
				.build();

			return chain.filter(exchange.mutate().request(mutatedRequest).build()).then(Mono.fromRunnable(() -> {
			}));
		};
	}

	private Mono<Void> errorResponse(JwtException e, ServerHttpResponse response) {
		log.debug("access token is invalid or logged out");
		try {
			return unAuthorizedResponse(response, e);
		} catch (JsonProcessingException ex) {
			throw new RuntimeException(ex);
		}
	}

	@Getter
	static class Config {
		//설정값이 필요하면 추가
		public static List<String> passPatterns = List.of(
			);
	}

	private Mono<Void> unAuthorizedResponse(ServerHttpResponse response, JwtException e) throws
		JsonProcessingException {
		response.setStatusCode(HttpStatus.UNAUTHORIZED);
		response.getHeaders().add(HttpHeaders.CONTENT_TYPE, "application/json");
		return response.writeWith(Mono.just(response.bufferFactory().wrap(
			objectMapper.writeValueAsBytes(new RsData<Void>("401", e.getMessage(), null)))));

	}

	private TokenInfo refreshAccessToken(RefreshToken refreshToken, Throwable expiredJwtException) {
		TokenInfo refreshedTokens = Util.refresh(refreshToken, Util.Key.convertSecretKey(jwtSecret),
			expiredJwtException);

		return refreshedTokens;
	}

	private RefreshToken getRefreshToken(ServerHttpRequest request) {
		String authCookie = getRefreshTokenCookie(request).getValue();
		RefreshToken refreshToken = Util.parseRefreshToken(authCookie);
		return refreshToken;
	}

	private static AccessToken getAccessToken(ServerHttpRequest request) {
		String authHeader = request.getHeaders().getFirst("Authorization");
		AccessToken accessToken = Util.parseAccessToken(authHeader);
		return accessToken;
	}

	private boolean checkWhiteList(Config config, ServerWebExchange exchange, GatewayFilterChain chain,
		ServerHttpRequest request) {
		if (whitelistProperties.getUrls().stream()
			.anyMatch(pattern -> new AntPathMatcher().match(pattern.getUrl(), request.getURI().getPath()) &&
				(pattern.getMethod().equals(request.getMethod().name()) || pattern.getMethod().equals("*"))) ) {
			log.debug("pass authorization filter");
			return true;
		}
		return false;
	}

	private String setCookieHeader(String name, String value, int maxAge, String path, boolean secure, boolean httpOnly,
		String sameSite) {
		String formatted = "%s=%s; Path=%s; Max-Age=%s; SameSite=%s".formatted(
			name, value, path, maxAge, sameSite
		);
		if (secure) {
			formatted += "; Secure";
		}
		if (httpOnly)
			formatted += "; HttpOnly";
		return formatted;
	}

	private HttpCookie getRefreshTokenCookie(ServerHttpRequest request) {
		if (request.getCookies().getFirst("refreshToken") == null) {
			throw new RuntimeException("refreshToken is null.");
		}
		return request.getCookies().getFirst("refreshToken");
	}
}

