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

@Aspect
@Component
public class RoleRequiredAspect {

	public RoleRequiredAspect(
		@Value("${custom.jwt.secret}") String key,
		HttpServletRequest request) {
		this.request = request;
		this.secretKey = key;
	}

	private final HttpServletRequest request;
	private final String secretKey;

	@Around("@annotation(roleRequired)")
	public Object acquireAuthentication(ProceedingJoinPoint joinPoint,RoleRequired roleRequired) throws Throwable {

		String authorization = getRequiredHeader("Authorization");
		AccessToken accessToken = Util.parseAccessToken(authorization);

		accessToken.decode(secretKey);

		String userId = getRequiredHeader("User-Id");
		Role role = Role.valueOf(getRequiredHeader("Role"));
		// boolean socialUser = request.getHeader("socialUser") != null;
		String email = getRequiredHeader("Email");

		if( !List.of(roleRequired.value()).contains(role))
			throw new AccessDeniedException("Access Denied");
		return joinPoint.proceed();
	}
	@Around("@annotation(AuthNeeded)")
	public Object setUserSecurityToken(ProceedingJoinPoint joinPoint) throws Throwable {
		String userId = getRequiredHeader("User-Id");
		Role role = Role.valueOf(getRequiredHeader("Role"));
		// boolean socialUser = request.getHeader("socialUser") != null;
		String email = getRequiredHeader("Email");
		try (LoggedInUserContext context = LoggedInUserContext.open(new UserSecurityToken(userId, email, role))
		) {
			return joinPoint.proceed();
		}
	}

	private String getRequiredHeader(String name) {
		String value = request.getHeader(name);
		if (value == null || value.isBlank()) {
			throw new IllegalArgumentException("Missing required header: " + name);
		}
		return value;
	}
}
