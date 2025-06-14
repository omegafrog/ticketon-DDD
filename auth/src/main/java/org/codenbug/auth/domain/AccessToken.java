package org.codenbug.auth.domain;

import java.time.Instant;
import java.util.Date;

import javax.crypto.SecretKey;

import org.codenbug.auth.global.Util;
import org.springframework.security.access.AccessDeniedException;

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
		claims = Util.getClaims(rawValue);
	}

	public static AccessToken refresh(UserId userId, Role role, boolean isSocialUser, String email, RefreshToken refreshToken,
		SecretKey secretKey) {
		refreshToken.verify(userId, secretKey);

		Claims payload = Jwts.claims()
			.add("userId", userId)
			.add("role", role)
			.add("email", email)
			.add("isSocialUser", isSocialUser)
			.build();

		String accessToken = Jwts.builder()
			.claims(payload)
			.expiration(Date.from(Instant.now().plusSeconds(60 * 30)))
			.signWith(secretKey, Jwts.SIG.HS256)
			.compact();

		return new AccessToken(accessToken, "Bearer");
	}

	public void decode() {
		claims = Util.getClaims(this.rawValue);
	}

	public Role getRole() {
		return claims.get("role", Role.class);
	}

	public UserId getUserId() {
		return new UserId(claims.get("userId", String.class));
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
