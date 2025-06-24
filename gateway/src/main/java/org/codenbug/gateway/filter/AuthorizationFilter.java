package org.codenbug.gateway.filter;

import java.util.List;

import org.codenbug.common.AccessToken;
import org.codenbug.common.RefreshToken;
import org.codenbug.common.Util;
import org.codenbug.common.exception.ExpiredJwtException;
import org.codenbug.common.exception.JwtException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpCookie;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Component
@Slf4j
public class AuthorizationFilter extends AbstractGatewayFilterFactory<AuthorizationFilter.Config> {
	@Value("${custom.jwt.secret}")
	private String jwtSecret;

	public AuthorizationFilter() {
		super(Config.class);
	}

	@Override
	public GatewayFilter apply(Config config) {
		return (exchange, chain) -> {
			ServerHttpRequest request = exchange.getRequest();
			ServerHttpResponse response = exchange.getResponse();

			if (config.passPatterns.stream()
				.anyMatch(pattern -> new AntPathMatcher().match(pattern, request.getURI().getPath()))) {
				log.debug("pass authorization filter");
				return chain.filter(exchange);
			}

			String authHeader = request.getHeaders().getFirst("Authorization");
			String authCookie = getRefreshTokenCookie(request).getValue();

			AccessToken accessToken = Util.parseAccessToken(authHeader);
			RefreshToken refreshToken = Util.parseRefreshToken(authCookie);
			try {
				Util.validate(accessToken.getRawValue(), Util.Key.convertSecretKey(jwtSecret));
			} catch (ExpiredJwtException e) {
				log.debug("access token is expired");
				accessToken = AccessToken.refresh(accessToken, refreshToken, Util.Key.convertSecretKey(jwtSecret));
				setToken(response, accessToken, refreshToken);
			} catch (JwtException e) {
				log.debug("access token is invalid");
				response.setStatusCode(org.springframework.http.HttpStatus.UNAUTHORIZED);
				return response.setComplete();
			}
			accessToken.decode(jwtSecret);
			ServerHttpRequest mutatedRequest = request.mutate()
				.header("User-Id", accessToken.getUserId())
				.header("Role", accessToken.getRole())
				.header("Email", accessToken.getEmail())
				.build();

			return chain.filter(exchange.mutate().request(mutatedRequest).build()).then(Mono.fromRunnable(() -> {
			}));
		};
	}

	@Getter
	static class Config {
		//설정값이 필요하면 추가
		public static List<String> passPatterns = List.of("/api/v1/users/register",
			"/api/v1/auth/login");
	}

	private void setToken(ServerHttpResponse response, AccessToken token, RefreshToken refreshToken) {
		response.getHeaders().add("Authorization", token.getType() + " " + token.getRawValue());
		response.getHeaders()
			.add(HttpHeaders.SET_COOKIE, setCookieHeader(
				"refreshToken", token.getRawValue(), 60 * 30, "/", true, true, "None"
			));
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

//
//
//
// }
