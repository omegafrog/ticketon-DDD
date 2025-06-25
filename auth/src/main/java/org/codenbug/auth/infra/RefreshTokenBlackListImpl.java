package org.codenbug.auth.infra;

import static org.codenbug.common.Util.*;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Date;

import org.codenbug.auth.domain.RefreshTokenBlackList;
import org.codenbug.common.Util;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import jakarta.servlet.http.Cookie;

@Repository
public class RefreshTokenBlackListImpl implements RefreshTokenBlackList {

	private final RedisTemplate<String, Object> redisTemplate;
	@Value("${custom.jwt.secret}")
	private String key;

	public RefreshTokenBlackListImpl(RedisTemplate<String, Object> redisTemplate) {
		this.redisTemplate = redisTemplate;
	}

	@Override
	public void add(String userId, Cookie refreshToken) {
		redisTemplate.opsForValue()
			.set("refreshToken:" + refreshToken.getValue(), userId, Duration.ofSeconds(REFRESH_TOKEN_EXP));
	}
}
