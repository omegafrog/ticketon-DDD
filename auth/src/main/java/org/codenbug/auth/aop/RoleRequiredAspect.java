package org.codenbug.auth.aop;

import java.util.Arrays;

import javax.crypto.SecretKey;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.codenbug.auth.domain.AccessToken;
import org.codenbug.auth.domain.RefreshToken;
import org.codenbug.auth.domain.Role;
import org.codenbug.auth.domain.UserId;
import org.codenbug.auth.domain.UserSecurityToken;
import org.codenbug.auth.global.Util;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Aspect
public class RoleRequiredAspect {

	public RoleRequiredAspect(
		@Value("${custom.jwt.key}") String key,
		HttpServletRequest request,
		HttpServletResponse response) {
		this.request = request;
		this.response = response;
		this.secretKey = Util.Key.convertSecretKey(key);
	}

	private final HttpServletRequest request;
	private final HttpServletResponse response;
	private final SecretKey secretKey;

	@Before("@annotation(roleRequired) || @within(roleRequired)")
	public void acquireAuthentication( RoleRequired roleRequired) throws Throwable {
		String authorization = request.getHeader("Authorization");
		AccessToken accessToken = Util.parseAccessToken(authorization);

		accessToken.decode();
		accessToken.checkSign(secretKey);

		UserId userId = accessToken.getUserId();
		Role role = accessToken.getRole();
		boolean socialUser = accessToken.isSocialUser();
		String email = accessToken.getEmail();

		if (accessToken.hasTokenExpired()) {
			RefreshToken refreshToken = Util.parseRefreshToken(request.getCookies());
			AccessToken reIssued = AccessToken.refresh(userId, role, socialUser, email, refreshToken, secretKey);

			response.setHeader(HttpHeaders.AUTHORIZATION, reIssued.getType() + " " + reIssued.getRawValue());
		}

		if(!Arrays.stream(roleRequired.value()).toList().contains(role)){
			throw new AccessDeniedException("you don't have correct permission");
		}
		SecurityContextHolder.getContext().setAuthentication(
			new UserSecurityToken(userId, email, role)
		);

	}
}
