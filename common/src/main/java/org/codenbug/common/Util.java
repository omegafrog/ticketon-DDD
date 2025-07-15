package org.codenbug.common;

import java.time.Instant;
import java.util.Date;
import java.util.Map;

import javax.crypto.SecretKey;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;

public class Util {

	private static final String ACCESS_TOKEN_COOKIE_NAME = "accessToken";
	private static final String REFRESH_TOKEN_COOKIE_NAME = "refreshToken";
	public static final long REFRESH_TOKEN_EXP = 60 * 60 * 24 * 7;

	public static RefreshToken parseRefreshToken(String cookies) {
		if(cookies == null){
			throw new IllegalArgumentException("RefreshToken Cookie is null");
		}
		return new RefreshToken(cookies);
	}

	/**
	 * Get Access token key-value string from Authorization header
	 * @param authHeader http header value that has "Authorization" as  key
	 * @return {@link AccessToken}
	 */
	public static AccessToken parseAccessToken(String authHeader) {
		if (authHeader == null || !authHeader.startsWith("Bearer ")) {
			throw new IllegalArgumentException("Invalid authorization header");
		}
		String[] s = authHeader.split(" ");
		return new AccessToken(s[1], s[0]);
	}

	public static Claims getClaims(String jwt, SecretKey secretKey) {
		return (Claims)Jwts.parser().verifyWith(secretKey).build()
			.parse(jwt)
			.getPayload();
	}

	public static void validate(String rawValue, SecretKey secretKey) {
		try {
			Jwts.parser().verifyWith(secretKey).build().parse(rawValue);
		}catch (ExpiredJwtException e){
			throw new org.codenbug.common.exception.ExpiredJwtException(
				"401", "Jwt is expired", e
			);
		}catch (SignatureException e){
			throw new org.codenbug.common.exception.SignatureException();
		}
	}

	public static TokenInfo refresh( RefreshToken refreshToken, SecretKey secretKey, Throwable e) {
		if(e instanceof ExpiredJwtException){
			ExpiredJwtException ex = (ExpiredJwtException)e;
			Claims claims = ex.getClaims();
			String userId = claims.get("userId", String.class);
			String role = claims.get("role", String.class);
			String email = claims.get("email", String.class);
			String jti = claims.get("jti", String.class);
			// boolean isSocialUser = claims.get("isSocialUser", Boolean.class);
			refreshToken.verify(jti, secretKey);

			Claims payload = Jwts.claims()
				.add("userId", userId)
				.add("role", role)
				.add("email", email)
				// .add("isSocialUser", isSocialUser)
				.build();

			String newtokenString = Jwts.builder()
				.claims(payload)
				.expiration(Date.from(Instant.now().plusSeconds(60 * 30)))
				.signWith(secretKey, Jwts.SIG.HS256)
				.compact();

			RefreshToken newRefreshToken = getRefreshToken(Map.of("userId", userId), secretKey);

			return new TokenInfo(new AccessToken(newtokenString, "Bearer"), newRefreshToken);
		}else{
			throw new RuntimeException("expired token's claim not usable.");
		}
	}

	public static class Key {
		public static SecretKey convertSecretKey(String key) {
			return Keys.hmacShaKeyFor(key.getBytes());
		}
	}
	public static TokenInfo generateTokens(Map<String, Object> claims, SecretKey secretKey) {
		return generateTokens(Jwts.claims(claims), secretKey);
	}

	public static TokenInfo generateTokens(Claims claims, SecretKey secretKey) {
		AccessToken accessToken = getAccessToken(claims, secretKey);

		RefreshToken refreshToken = getRefreshToken(Map.of("access-jti", accessToken.getClaims().getId()), secretKey);

		return new TokenInfo(accessToken, refreshToken);
	}

	private static RefreshToken getRefreshToken(Map<String, Object> claims, SecretKey secretKey) {

		String refreshvalue = Jwts.builder()
			.claims(claims)
			.signWith(secretKey, Jwts.SIG.HS256)
			.expiration(Date.from(Instant.now().plusSeconds(60 * 60 * 24 * 7)))
			.compact();

		RefreshToken refreshToken = new RefreshToken(refreshvalue);
		return refreshToken;
	}

	private static AccessToken getAccessToken(Claims claims, SecretKey secretKey) {
		String token = Jwts.builder()
			.claims(claims)
			.signWith(secretKey, Jwts.SIG.HS256)
			.id(java.util.UUID.randomUUID().toString())
			.expiration(Date.from(Instant.now().plusSeconds(60 * 1)))
			.compact();
		AccessToken accessToken = new AccessToken(token, "Bearer");
		Claims payload = (Claims)Jwts.parser()
			.verifyWith(secretKey)
			.build()
			.parse(accessToken.getRawValue())
			.getPayload();
		accessToken.setClaims(payload);
		return accessToken;
	}


}
