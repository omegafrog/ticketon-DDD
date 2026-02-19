package org.codenbug.gateway.infra;

import org.codenbug.common.RefreshToken;
import org.codenbug.common.exception.BlacklistedRefreshTokenException;
import org.codenbug.gateway.filter.RedisRefreshTokenBlackList;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.stereotype.Repository;

import reactor.core.publisher.Mono;

@Repository
public class RedisRefreshTokenBlackListImpl implements RedisRefreshTokenBlackList {

	private final ReactiveStringRedisTemplate redisTemplate;

	public RedisRefreshTokenBlackListImpl(ReactiveStringRedisTemplate redisTemplate) {
		this.redisTemplate = redisTemplate;
	}

	@Override
	public Mono<Void> checkBlackList(RefreshToken refreshToken) {
		return redisTemplate.hasKey("refreshToken:" + refreshToken.getValue())
			.flatMap(isBlacklisted -> {
				if (Boolean.TRUE.equals(isBlacklisted)) {
					return Mono.error(new BlacklistedRefreshTokenException("Refresh token is blacklisted."));
				}
				return Mono.empty();
			});
	}
}
