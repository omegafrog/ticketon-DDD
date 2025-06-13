package org.codenbug.auth.aop;

import java.security.PublicKey;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.codenbug.auth.domain.AccessToken;
import org.codenbug.auth.domain.RefreshToken;
import org.codenbug.auth.domain.Role;
import org.codenbug.auth.domain.TokenInfo;
import org.codenbug.auth.domain.UserId;
import org.codenbug.auth.global.Util;
import org.springframework.beans.factory.annotation.Value;

import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;

@Aspect
public class RoleRequiredAspect {

	public RoleRequiredAspect(@Value("${custom.jwt.publicKey}") String publicKey, HttpServletRequest request) {
		this.request = request;
		this.publicKey = Util.Key.getPublicKeyFromBase64(publicKey);
	}

	private final HttpServletRequest request;
	private final PublicKey publicKey;

	@Around("@annotation(RoleRequired) || within(RoleRequired)")
	public Object acquireAuthentication(ProceedingJoinPoint joinPoint) {
		String authorization = request.getHeader("Authorization");
		AccessToken accessToken = Util.parseAccessToken(authorization);
		Util.hasTokenExpired(accessToken);
		Role role = Util.getRole(accessToken, publicKey);
		UserId userId = Util.getUserId(accessToken, publicKey);

		RefreshToken refreshToken = Util.parseRefreshToken(request.getCookies());

	}
}
