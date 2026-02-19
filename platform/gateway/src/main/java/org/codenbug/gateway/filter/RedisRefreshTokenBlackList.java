package org.codenbug.gateway.filter;

import org.codenbug.common.RefreshToken;

import reactor.core.publisher.Mono;

public interface RedisRefreshTokenBlackList {

	Mono<Void> checkBlackList(RefreshToken refreshToken);
}
