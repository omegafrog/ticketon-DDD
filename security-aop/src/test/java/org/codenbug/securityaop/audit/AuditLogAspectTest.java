package org.codenbug.securityaop.audit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;

import org.aspectj.lang.ProceedingJoinPoint;
import org.codenbug.common.Role;
import org.codenbug.common.RsData;
import org.codenbug.securityaop.aop.LoggedInUserContext;
import org.codenbug.securityaop.aop.UserSecurityToken;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.fasterxml.jackson.databind.ObjectMapper;

class AuditLogAspectTest {

	@AfterEach
	void tearDown() {
		RequestContextHolder.resetRequestAttributes();
		LoggedInUserContext.clear();
	}

	@Test
	void auditSecuredApi_savesRequestResponseAndUser() throws Throwable {
		InMemoryAuditLogSink sink = new InMemoryAuditLogSink(
			Clock.fixed(Instant.parse("2026-04-28T00:00:00Z"), ZoneOffset.UTC),
			Duration.ofDays(30));
		AuditLogAspect aspect = new AuditLogAspect(sink, new ObjectMapper(),
			Clock.fixed(Instant.parse("2026-04-28T00:00:00Z"), ZoneOffset.UTC));
		ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
		when(joinPoint.getArgs()).thenReturn(new Object[] {Map.of("name", "user", "password", "raw-password")});
		when(joinPoint.proceed()).thenReturn(new RsData<>("200", "ok", Map.of("accessToken", "raw-token")));

		MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/users/me");
		RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

		try (LoggedInUserContext ignored = LoggedInUserContext.open(
			new UserSecurityToken("user-1", "user-1@ticketon.site", Role.USER))) {
			aspect.auditSecuredApi(joinPoint);
		}

		assertThat(sink.records()).hasSize(1);
		AuditLogRecord record = sink.records().get(0);
		assertThat(record.httpMethod()).isEqualTo("POST");
		assertThat(record.path()).isEqualTo("/api/v1/users/me");
		assertThat(record.userId()).isEqualTo("user-1");
		assertThat(record.email()).isEqualTo("user-1@ticketon.site");
		assertThat(record.role()).isEqualTo(Role.USER);
		assertThat(record.success()).isTrue();
		assertThat(record.requestPayload()).contains("[REDACTED]");
		assertThat(record.requestPayload()).doesNotContain("raw-password");
		assertThat(record.responsePayload()).contains("[REDACTED]");
		assertThat(record.responsePayload()).doesNotContain("raw-token");
	}

	@Test
	void auditSecuredApi_savesFailureWithoutSecret() throws Throwable {
		InMemoryAuditLogSink sink = new InMemoryAuditLogSink(
			Clock.fixed(Instant.parse("2026-04-28T00:00:00Z"), ZoneOffset.UTC),
			Duration.ofDays(30));
		AuditLogAspect aspect = new AuditLogAspect(sink, new ObjectMapper(),
			Clock.fixed(Instant.parse("2026-04-28T00:00:00Z"), ZoneOffset.UTC));
		ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
		when(joinPoint.getArgs()).thenReturn(new Object[] {});
		when(joinPoint.proceed()).thenThrow(new IllegalArgumentException("Authorization=Bearer secret-token"));

		MockHttpServletRequest request = new MockHttpServletRequest("DELETE", "/api/v1/events/event-1");
		request.addHeader("User-Id", "manager-1");
		request.addHeader("Email", "manager-1@ticketon.site");
		request.addHeader("Role", "MANAGER");
		RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

		org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
			() -> aspect.auditSecuredApi(joinPoint));

		AuditLogRecord record = sink.records().get(0);
		assertThat(record.success()).isFalse();
		assertThat(record.userId()).isEqualTo("manager-1");
		assertThat(record.role()).isEqualTo(Role.MANAGER);
		assertThat(record.errorType()).isEqualTo("IllegalArgumentException");
		assertThat(record.errorMessage()).contains("[REDACTED]");
		assertThat(record.errorMessage()).doesNotContain("secret-token");
	}

	@Test
	void inMemorySink_removesRecordsOlderThanOneMonthRetention() {
		Clock clock = Clock.fixed(Instant.parse("2026-04-28T00:00:00Z"), ZoneOffset.UTC);
		InMemoryAuditLogSink sink = new InMemoryAuditLogSink(clock, Duration.ofDays(30));
		sink.save(recordAt(Instant.parse("2026-03-01T00:00:00Z")));

		sink.save(recordAt(Instant.parse("2026-04-28T00:00:00Z")));

		assertThat(sink.records()).extracting(AuditLogRecord::occurredAt)
			.containsExactly(Instant.parse("2026-04-28T00:00:00Z"));
	}

	private AuditLogRecord recordAt(Instant occurredAt) {
		return new AuditLogRecord(occurredAt, "GET", "/api/v1/users/me", "user-1",
			"user-1@ticketon.site", Role.USER, "{}", "{}", true, null, null);
	}
}
