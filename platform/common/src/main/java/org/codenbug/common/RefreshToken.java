package org.codenbug.common;

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
	 * @param accessTokenJti access token's jti
	 * @param secretKey key to sign signature
	 */
	public void verify(String accessTokenJti, SecretKey secretKey) {
		Jwts.parser()
			.verifyWith(secretKey)
			.require("access-jti", accessTokenJti)
			.build()
			.parse(value);
	}


}
