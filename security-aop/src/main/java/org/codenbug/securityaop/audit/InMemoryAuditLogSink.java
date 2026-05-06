package org.codenbug.securityaop.audit;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class InMemoryAuditLogSink implements AuditLogSink {
	private final CopyOnWriteArrayList<AuditLogRecord> records = new CopyOnWriteArrayList<>();
	private final Clock clock;
	private final Duration retention;

	@Autowired
	public InMemoryAuditLogSink(
		@Value("${audit.retention-days:30}") long retentionDays
	) {
		this(Clock.systemUTC(), Duration.ofDays(retentionDays));
	}

	InMemoryAuditLogSink(Clock clock, Duration retention) {
		this.clock = clock;
		this.retention = retention;
	}

	@Override
	public void save(AuditLogRecord record) {
		purgeOlderThan(clock.instant().minus(retention));
		records.add(record);
	}

	@Override
	public void purgeOlderThan(Instant cutoff) {
		records.removeIf(record -> record.occurredAt().isBefore(cutoff));
	}

	public List<AuditLogRecord> records() {
		return List.copyOf(records);
	}
}
