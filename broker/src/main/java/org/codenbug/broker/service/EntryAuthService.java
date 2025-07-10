package org.codenbug.broker.service;

import java.time.Instant;
import java.util.Date;
import java.util.Map;

import org.codenbug.common.Util;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import io.jsonwebtoken.Jwts;

@Service
public class EntryAuthService {
	@Value("${custom.jwt.secret}")
	private String secret;
	@Value("${custom.jwt.expiration}")
	private long expiration;

	public String generateEntryAuthToken(Map<String, Object> claims, String subject) {
		return Jwts.builder().signWith(Util.Key.convertSecretKey(secret))
			.setClaims(claims)
			.claim("subject", subject)
			.setExpiration(Date.from(Instant.now().plusSeconds(expiration)))
			.compact();
	}

}
