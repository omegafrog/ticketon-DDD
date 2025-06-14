package org.codenbug.auth.global;

import java.util.Arrays;

import javax.crypto.SecretKey;

import org.codenbug.auth.domain.AccessToken;
import org.codenbug.auth.domain.RefreshToken;
import org.springframework.security.access.AccessDeniedException;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.Cookie;

public class Util {

	private static final String ACCESS_TOKEN_COOKIE_NAME = "accessToken";
	private static final String REFRESH_TOKEN_COOKIE_NAME = "refreshToken";

	public static RefreshToken parseRefreshToken(Cookie[] cookies) {
		String refreshTokenValue = Arrays.stream(cookies)
			.filter(item -> item.getName().equals(REFRESH_TOKEN_COOKIE_NAME
			))
			.findFirst()
			.orElseThrow(() -> new IllegalArgumentException("Cannot find refresh token from cookies"))
			.getValue();

		return new RefreshToken(refreshTokenValue);
	}

	/**
	 * Get Access token key-value string from Authorization header
	 * @param authHeader http header value that has "Authorization" as  key
	 * @return {@link AccessToken}
	 */
	public static AccessToken parseAccessToken(String authHeader) {
		String[] s = authHeader.split(" ");
		return new AccessToken(s[1], s[0]);
	}

	public static Claims getClaims(String jwt) {
		return (Claims)Jwts.parser().build()
			.parse(jwt)
			.getPayload();
	}

	public static void validate(String rawValue, SecretKey secretKey) {
		try{
			Jwts.parser().verifyWith(secretKey).build().parse(rawValue);
		}catch (RuntimeException e){
			throw new AccessDeniedException("Token is invalid");
		}
	}

	public static class Key {
		public static SecretKey convertSecretKey(String key) {
			return Keys.hmacShaKeyFor(key.getBytes());
		}
	}

}
