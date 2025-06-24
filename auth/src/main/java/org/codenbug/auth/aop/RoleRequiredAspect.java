package org.codenbug.auth.aop;

import java.util.Arrays;

import javax.crypto.SecretKey;

import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.codenbug.common.AccessToken;
import org.codenbug.common.RefreshToken;
import org.codenbug.auth.domain.Role;
import org.codenbug.auth.domain.UserId;
import org.codenbug.auth.domain.UserSecurityToken;
import org.codenbug.common.Util;
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
		this.secretKey = key;
	}

	private final HttpServletRequest request;
	private final HttpServletResponse response;
	private final String secretKey;

	@Before("@annotation(roleRequired) || @within(roleRequired)")
	public void acquireAuthentication(RoleRequired roleRequired) throws Throwable {
		String authorization = request.getHeader("Authorization");
		AccessToken accessToken = Util.parseAccessToken(authorization);

		accessToken.decode(secretKey);

		UserId userId = new UserId(accessToken.getUserId());
		Role role = Role.valueOf(accessToken.getRole());
		boolean socialUser = accessToken.isSocialUser();
		String email = accessToken.getEmail();

		if (accessToken.hasTokenExpired()) {
			RefreshToken refreshToken = Util.parseRefreshToken(
				Arrays.stream(request.getCookies())
					.filter(cookie -> cookie.getName().equals("refreshToken"))
					.findFirst()
					.orElseThrow(() -> new AccessDeniedException("refresh token is not found"))
					.getValue());
			AccessToken reIssued = AccessToken.refresh(accessToken, refreshToken, Util.Key.convertSecretKey(secretKey));

			response.setHeader(HttpHeaders.AUTHORIZATION, reIssued.getType() + " " + reIssued.getRawValue());
		}

		if (!Arrays.stream(roleRequired.value()).toList().contains(role)) {
			throw new AccessDeniedException("you don't have correct permission");
		}
		SecurityContextHolder.getContext().setAuthentication(
			new UserSecurityToken(userId, email, role)
		);

	}
}
