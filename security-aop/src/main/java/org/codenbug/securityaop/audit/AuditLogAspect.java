package org.codenbug.securityaop.audit;

import java.time.Clock;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.codenbug.common.Role;
import org.codenbug.securityaop.aop.LoggedInUserContext;
import org.codenbug.securityaop.aop.UserSecurityToken;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;

@Aspect
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 50)
public class AuditLogAspect {
	private static final Set<String> SECRET_KEYS = Set.of(
		"authorization", "cookie", "set-cookie", "password", "accessToken", "refreshToken", "token",
		"entryAuthToken", "secret"
	);

	private final AuditLogSink auditLogSink;
	private final ObjectMapper objectMapper;
	private final Clock clock;

	@Autowired
	public AuditLogAspect(AuditLogSink auditLogSink, ObjectMapper objectMapper) {
		this(auditLogSink, objectMapper, Clock.systemUTC());
	}

	AuditLogAspect(AuditLogSink auditLogSink, ObjectMapper objectMapper, Clock clock) {
		this.auditLogSink = auditLogSink;
		this.objectMapper = objectMapper;
		this.clock = clock;
	}

	@Around("@annotation(org.codenbug.securityaop.aop.AuthNeeded) || @annotation(org.codenbug.securityaop.aop.RoleRequired)")
	public Object auditSecuredApi(ProceedingJoinPoint joinPoint) throws Throwable {
		Instant occurredAt = clock.instant();
		HttpServletRequest request = currentRequest();
		try {
			Object response = joinPoint.proceed();
			saveRecord(occurredAt, request, joinPoint.getArgs(), response, true, null);
			return response;
		} catch (Throwable throwable) {
			saveRecord(occurredAt, request, joinPoint.getArgs(), null, false, throwable);
			throw throwable;
		}
	}

	private void saveRecord(Instant occurredAt, HttpServletRequest request, Object[] args, Object response,
		boolean success, Throwable throwable) {
		AuditPrincipal principal = resolvePrincipal(request);
		auditLogSink.save(new AuditLogRecord(
			occurredAt,
			request == null ? null : request.getMethod(),
			request == null ? null : request.getRequestURI(),
			principal.userId(),
			principal.email(),
			principal.role(),
			serializeArguments(args),
			serialize(response),
			success,
			throwable == null ? null : throwable.getClass().getSimpleName(),
			throwable == null ? null : redact(throwable.getMessage())
		));
	}

	private AuditPrincipal resolvePrincipal(HttpServletRequest request) {
		UserSecurityToken token = LoggedInUserContext.get();
		if (token != null) {
			return new AuditPrincipal(token.getUserId(), token.getEmail(), token.getRole());
		}
		if (request == null) {
			return new AuditPrincipal(null, null, null);
		}
		Role role = null;
		String roleHeader = request.getHeader("Role");
		if (roleHeader != null && !roleHeader.isBlank()) {
			role = Role.valueOf(roleHeader);
		}
		return new AuditPrincipal(request.getHeader("User-Id"), request.getHeader("Email"), role);
	}

	private HttpServletRequest currentRequest() {
		if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes) {
			return attributes.getRequest();
		}
		return null;
	}

	private String serializeArguments(Object[] args) {
		List<Object> auditableArgs = Arrays.stream(args)
			.filter(arg -> !(arg instanceof ServletRequest))
			.filter(arg -> !(arg instanceof ServletResponse))
			.filter(arg -> !(arg instanceof MultipartFile))
			.collect(Collectors.toList());
		return serialize(auditableArgs);
	}

	private String serialize(Object value) {
		if (value == null) {
			return null;
		}
		try {
			return redact(objectMapper.writeValueAsString(value));
		} catch (JsonProcessingException e) {
			return redact(String.valueOf(value));
		}
	}

	private String redact(String raw) {
		if (raw == null) {
			return null;
		}
		String redacted = raw;
		redacted = redacted.replaceAll("(?i)(authorization\\s*=\\s*)bearer\\s+[^,}\\s]+", "$1Bearer [REDACTED]");
		for (String key : SECRET_KEYS) {
			redacted = redacted.replaceAll("(?i)(\"" + key + "\"\\s*:\\s*\")[^\"]*(\")", "$1[REDACTED]$2");
			redacted = redacted.replaceAll("(?i)(" + key + "\\s*=\\s*)[^,}\\s]+", "$1[REDACTED]");
		}
		if (redacted.toLowerCase(Locale.ROOT).contains("bearer ")) {
			redacted = redacted.replaceAll("(?i)bearer\\s+[^\\s,}\\]\"]+", "Bearer [REDACTED]");
		}
		return redacted;
	}

	private record AuditPrincipal(String userId, String email, Role role) {
	}
}
