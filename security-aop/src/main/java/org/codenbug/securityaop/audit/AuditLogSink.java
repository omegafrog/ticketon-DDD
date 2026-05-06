package org.codenbug.securityaop.audit;

import java.time.Instant;

public interface AuditLogSink {
	void save(AuditLogRecord record);

	void purgeOlderThan(Instant cutoff);
}
