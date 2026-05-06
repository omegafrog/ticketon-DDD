package org.codenbug.securityaop.audit;

import java.time.Instant;

import org.codenbug.common.Role;

public record AuditLogRecord(
	Instant occurredAt,
	String httpMethod,
	String path,
	String userId,
	String email,
	Role role,
	String requestPayload,
	String responsePayload,
	boolean success,
	String errorType,
	String errorMessage
) {
}
