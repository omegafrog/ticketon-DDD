package org.codenbug.purchase.domain.es;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;

@Entity
@Getter
@Table(
	name = "purchase_event_store",
	uniqueConstraints = {
		@UniqueConstraint(name = "uq_purchase_event_store_purchase_seq", columnNames = {"purchase_id", "seq"}),
		@UniqueConstraint(name = "uq_purchase_event_store_purchase_command", columnNames = {"purchase_id", "command_id"})
	}
)
public class PurchaseStoredEvent {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "purchase_id", nullable = false, length = 64)
	private String purchaseId;

	@Column(name = "seq", nullable = false)
	private long seq;

	@Column(name = "event_type", nullable = false, length = 64)
	private String eventType;

	@Column(name = "command_id", nullable = false, length = 128)
	private String commandId;

	@Lob
	@Column(name = "payload_json", nullable = false)
	private String payloadJson;

	@Lob
	@Column(name = "metadata_json", nullable = false)
	private String metadataJson;

	@Column(name = "occurred_at", nullable = false)
	private LocalDateTime occurredAt;

	protected PurchaseStoredEvent() {}

	public PurchaseStoredEvent(String purchaseId, long seq, String eventType, String commandId, String payloadJson,
		String metadataJson, LocalDateTime occurredAt) {
		this.purchaseId = purchaseId;
		this.seq = seq;
		this.eventType = eventType;
		this.commandId = commandId;
		this.payloadJson = payloadJson;
		this.metadataJson = metadataJson;
		this.occurredAt = occurredAt;
	}
}
