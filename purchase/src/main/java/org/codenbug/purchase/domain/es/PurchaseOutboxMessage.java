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
	name = "purchase_outbox",
	uniqueConstraints = {
		@UniqueConstraint(name = "uq_purchase_outbox_message_id", columnNames = {"message_id"})
	}
)
public class PurchaseOutboxMessage {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "message_id", nullable = false, length = 64)
	private String messageId;

	@Column(name = "queue_name", nullable = false, length = 128)
	private String queueName;

	@Lob
	@Column(name = "payload_json", nullable = false)
	private String payloadJson;

	@Column(name = "created_at", nullable = false)
	private LocalDateTime createdAt;

	@Column(name = "published_at")
	private LocalDateTime publishedAt;

	@Column(name = "publish_attempts", nullable = false)
	private int publishAttempts;

	@Lob
	@Column(name = "last_error")
	private String lastError;

	protected PurchaseOutboxMessage() {}

	public static PurchaseOutboxMessage of(String messageId, String queueName, String payloadJson, LocalDateTime now) {
		PurchaseOutboxMessage msg = new PurchaseOutboxMessage();
		msg.messageId = messageId;
		msg.queueName = queueName;
		msg.payloadJson = payloadJson;
		msg.createdAt = now;
		msg.publishAttempts = 0;
		return msg;
	}

	public void markPublishAttemptFailed(String error) {
		this.publishAttempts++;
		this.lastError = error;
	}

	public void markPublished(LocalDateTime now) {
		this.publishAttempts++;
		this.publishedAt = now;
		this.lastError = null;
	}
}
