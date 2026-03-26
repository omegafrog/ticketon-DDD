package org.codenbug.event.domain;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;

@Entity
@Getter
@Table(
	name = "event_payment_hold",
	indexes = {
		@Index(name = "idx_event_payment_hold_event_id", columnList = "event_id"),
		@Index(name = "idx_event_payment_hold_active", columnList = "event_id,status,expires_at")
	}
)
public class EventPaymentHold {
	@Id
	@Column(name = "hold_token", length = 64)
	private String holdToken;

	@Column(name = "event_id", nullable = false, length = 64)
	private String eventId;

	@Column(name = "purchase_id", nullable = false, length = 64)
	private String purchaseId;

	@Column(name = "expires_at", nullable = false)
	private LocalDateTime expiresAt;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false, length = 16)
	private EventPaymentHoldStatus status;

	@Column(name = "created_at", nullable = false)
	private LocalDateTime createdAt;

	protected EventPaymentHold() {}

	public static EventPaymentHold active(String holdToken, String eventId, String purchaseId, LocalDateTime expiresAt,
		LocalDateTime now) {
		EventPaymentHold hold = new EventPaymentHold();
		hold.holdToken = holdToken;
		hold.eventId = eventId;
		hold.purchaseId = purchaseId;
		hold.expiresAt = expiresAt;
		hold.status = EventPaymentHoldStatus.ACTIVE;
		hold.createdAt = now;
		return hold;
	}

	public boolean isActiveAt(LocalDateTime now) {
		return status == EventPaymentHoldStatus.ACTIVE && expiresAt.isAfter(now);
	}

	public void consume(LocalDateTime now) {
		if (!isActiveAt(now)) {
			throw new IllegalStateException("Hold is not active");
		}
		this.status = EventPaymentHoldStatus.CONSUMED;
	}

	public void release(LocalDateTime now) {
		if (!isActiveAt(now)) {
			throw new IllegalStateException("Hold is not active");
		}
		this.status = EventPaymentHoldStatus.RELEASED;
	}
}
