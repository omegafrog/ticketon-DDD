package org.codenbug.purchase.domain.es;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;

@Entity
@Getter
@Table(name = "purchase_confirm_status")
public class PurchaseConfirmStatusProjection {
	@Id
	@Column(name = "purchase_id", length = 64)
	private String purchaseId;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false, length = 16)
	private PurchaseConfirmStatus status;

	@Column(name = "last_event_seq", nullable = false)
	private long lastEventSeq;

	@Column(name = "last_event_type", nullable = false, length = 64)
	private String lastEventType;

	@Column(name = "message", length = 255)
	private String message;

	@Column(name = "updated_at", nullable = false)
	private LocalDateTime updatedAt;

	protected PurchaseConfirmStatusProjection() {}

	public PurchaseConfirmStatusProjection(String purchaseId, PurchaseConfirmStatus status, long lastEventSeq,
		String lastEventType, String message, LocalDateTime updatedAt) {
		this.purchaseId = purchaseId;
		this.status = status;
		this.lastEventSeq = lastEventSeq;
		this.lastEventType = lastEventType;
		this.message = message;
		this.updatedAt = updatedAt;
	}

	public static PurchaseConfirmStatusProjection pending(String purchaseId, long lastEventSeq, String lastEventType,
		LocalDateTime now) {
		return new PurchaseConfirmStatusProjection(purchaseId, PurchaseConfirmStatus.PENDING, lastEventSeq, lastEventType,
			"accepted", now);
	}

	public void update(PurchaseConfirmStatus status, long lastEventSeq, String lastEventType, String message,
		LocalDateTime now) {
		this.status = status;
		this.lastEventSeq = lastEventSeq;
		this.lastEventType = lastEventType;
		this.message = message;
		this.updatedAt = now;
	}
}
