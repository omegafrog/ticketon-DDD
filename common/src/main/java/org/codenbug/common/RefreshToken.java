package org.codenbug.common;

import java.time.Instant;
import java.util.Date;

import javax.crypto.SecretKey;

import io.jsonwebtoken.Jwts;
import lombok.Getter;

@Getter
public class RefreshToken {
	private String value;

	protected RefreshToken() {}

	public  RefreshToken(String value) {
		this.value = value;
	}

	/**
	 * verify the refreshToken to ensure it matches the given userId and was issued after the date
	 * @param userId verifying user's id
	 * @param secretKey key to sign signature
	 */
	public void verify(String userId, SecretKey secretKey) {
		Jwts.parser()
			.verifyWith(secretKey)
			.require("userId", userId)
			.requireNotBefore(Date.from(Instant.now())).build()
			.parse(value);
	}
}
