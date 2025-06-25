package org.codenbug.auth.domain;

import jakarta.servlet.http.Cookie;

public interface RefreshTokenBlackList {
	void add(String userId, Cookie refreshToken);
}
