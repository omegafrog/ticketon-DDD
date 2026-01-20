package org.codenbug.gateway.infra;

import org.codenbug.common.RefreshToken;
import org.codenbug.common.exception.BlacklistedRefreshTokenException;
import org.codenbug.gateway.filter.RedisRefreshTokenBlackList;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class RedisRefreshTokenBlackListImpl implements RedisRefreshTokenBlackList {

	private final RedisTemplate<String, Object> redisTemplate;

	public RedisRefreshTokenBlackListImpl(RedisTemplate<String, Object> redisTemplate) {
		this.redisTemplate = redisTemplate;
	}

	@Override
	public void checkBlackList(RefreshToken refreshToken) {
		if (redisTemplate.hasKey("refreshToken:" + refreshToken.getValue()))
			throw new BlacklistedRefreshTokenException("Refresh token is blacklisted.");
	}
}
