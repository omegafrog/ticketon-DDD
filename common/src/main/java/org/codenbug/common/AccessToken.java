package org.codenbug.common;

import java.time.Instant;
import java.util.Date;
import java.util.LinkedHashMap;

import javax.crypto.SecretKey;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import lombok.Getter;

@Getter
public class AccessToken {
	private String rawValue;
	private String type;
	private Claims claims;

	protected AccessToken() {
	}

	public AccessToken(String rawValue, String type) {
		this.rawValue = rawValue;
		this.type = type;
	}

	public static AccessToken refresh(AccessToken accessToken, RefreshToken refreshToken,
		SecretKey secretKey) {
		String userId = accessToken.getClaims().get("userId", String.class);
		String role = accessToken.getClaims().get("role", String.class);
		String email = accessToken.getClaims().get("email", String.class);
		boolean isSocialUser = accessToken.getClaims().get("isSocialUser", Boolean.class);
		refreshToken.verify(userId, secretKey);

		Claims payload = Jwts.claims()
			.add("userId", userId)
			.add("role", role)
			.add("email", email)
			.add("isSocialUser", isSocialUser)
			.build();

		String newtokenString = Jwts.builder()
			.claims(payload)
			.expiration(Date.from(Instant.now().plusSeconds(60 * 30)))
			.signWith(secretKey, Jwts.SIG.HS256)
			.compact();

		return new AccessToken(newtokenString, "Bearer");
	}

	public AccessToken decode(String key) {
		this.claims = Util.getClaims(this.rawValue, Util.Key.convertSecretKey(key));
		return this;
	}

	public String getRole() {
		return claims.get("role", String.class);
	}

	public String getUserId() {
		return claims.get("userId", String.class);
	}

	public boolean isSocialUser() {
		return claims.get("isSocialUser", Boolean.class);
	}

	public String getEmail() {
		return claims.get("email", String.class);
	}

	/**
	 * if the access token is expired, return true
	 * @return
	 */
	public boolean hasTokenExpired() {
		return Instant.now().isAfter(claims.getExpiration().toInstant());
	}

	public void checkSign(SecretKey secretKey) {
		Util.validate(this.rawValue, secretKey);
	}
}
