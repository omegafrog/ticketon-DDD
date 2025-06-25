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
