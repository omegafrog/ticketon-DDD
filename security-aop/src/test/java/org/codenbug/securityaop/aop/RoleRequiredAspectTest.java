package org.codenbug.securityaop.aop;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Map;

import org.aspectj.lang.ProceedingJoinPoint;
import org.codenbug.common.Role;
import org.codenbug.common.TokenInfo;
import org.codenbug.common.Util;
import org.codenbug.common.exception.AccessDeniedException;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

class RoleRequiredAspectTest {
	private static final String JWT_SECRET = "testSecretKeyForJwtTokenGenerationThatIsLongEnough";

	@Test
	void 인증_필요한_MANAGER_역할_허용() throws Throwable {
		MockHttpServletRequest request = requestWithRole(Role.MANAGER);
		RoleRequiredAspect aspect = new RoleRequiredAspect(JWT_SECRET, request);
		ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
		when(joinPoint.proceed()).thenReturn("ok");
		RoleRequired roleRequired = roleRequired(Role.MANAGER, Role.ADMIN);

		Object result = aspect.acquireAuthentication(joinPoint, roleRequired);

		assertThat(result).isEqualTo("ok");
	}

	@Test
	void 인증_MANAGER_필요_시_MEMBER_거부() {
		MockHttpServletRequest request = requestWithRole(Role.USER);
		RoleRequiredAspect aspect = new RoleRequiredAspect(JWT_SECRET, request);
		ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
		RoleRequired roleRequired = roleRequired(Role.MANAGER);

		assertThatThrownBy(() -> aspect.acquireAuthentication(joinPoint, roleRequired))
			.isInstanceOf(AccessDeniedException.class);
	}

	@Test
	void 사용자_보안_토큰_설정_컨텍스트_열고_닫기() throws Throwable {
		MockHttpServletRequest request = requestWithRole(Role.ADMIN);
		RoleRequiredAspect aspect = new RoleRequiredAspect(JWT_SECRET, request);
		ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
		when(joinPoint.proceed()).thenAnswer(invocation -> {
			UserSecurityToken token = LoggedInUserContext.get();
			assertThat(token.getUserId()).isEqualTo("user-1");
			assertThat(token.getEmail()).isEqualTo("user-1@ticketon.site");
			assertThat(token.getRole()).isEqualTo(Role.ADMIN);
			return "ok";
		});

		Object result = aspect.setUserSecurityToken(joinPoint);

		assertThat(result).isEqualTo("ok");
		assertThat(LoggedInUserContext.get()).isNull();
	}

	private MockHttpServletRequest requestWithRole(Role role) {
		TokenInfo tokenInfo = Util.generateTokens(
			Map.of("userId", "user-1", "role", role.name(), "email", "user-1@ticketon.site"),
			Util.Key.convertSecretKey(JWT_SECRET));
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addHeader("Authorization",
			tokenInfo.getAccessToken().getType() + " " + tokenInfo.getAccessToken().getRawValue());
		request.addHeader("User-Id", "user-1");
		request.addHeader("Email", "user-1@ticketon.site");
		request.addHeader("Role", role.name());
		return request;
	}

	private RoleRequired roleRequired(Role... roles) {
		RoleRequired roleRequired = mock(RoleRequired.class);
		when(roleRequired.value()).thenReturn(roles);
		return roleRequired;
	}
}
