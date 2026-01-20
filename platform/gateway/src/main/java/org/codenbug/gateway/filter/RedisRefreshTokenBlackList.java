package org.codenbug.gateway.filter;

import org.codenbug.common.RefreshToken;

public interface RedisRefreshTokenBlackList {

	void checkBlackList(RefreshToken refreshToken);
}
