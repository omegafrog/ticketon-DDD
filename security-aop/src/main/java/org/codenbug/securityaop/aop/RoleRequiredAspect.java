package org.codenbug.securityaop.aop;

import java.util.List;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.codenbug.common.AccessToken;
import org.codenbug.common.Role;
import org.codenbug.common.Util;
import org.codenbug.common.exception.AccessDeniedException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Aspect
@Component
public class RoleRequiredAspect {

	public RoleRequiredAspect(
		@Value("${custom.jwt.secret}") String key,
		HttpServletRequest request,
		HttpServletResponse response) {
		this.request = request;
		this.response = response;
		this.secretKey = key;
	}

	private final HttpServletRequest request;
	private final HttpServletResponse response;
	private final String secretKey;

	@Around("@annotation(roleRequired)")
	public Object acquireAuthentication(ProceedingJoinPoint joinPoint,RoleRequired roleRequired) throws Throwable {

		String authorization = request.getHeader("Authorization");
		AccessToken accessToken = Util.parseAccessToken(authorization);

		accessToken.decode(secretKey);

		String userId = request.getHeader("User-Id");
		Role role = Role.valueOf(request.getHeader("Role"));
		// boolean socialUser = request.getHeader("socialUser") != null;
		String email = request.getHeader("Email");

		if( !List.of(roleRequired.value()).contains(role))
			throw new AccessDeniedException("Access Denied");

		try (LoggedInUserContext context = LoggedInUserContext.open(new UserSecurityToken(userId, email, role))
		) {
			return joinPoint.proceed();
		}

	}
}
