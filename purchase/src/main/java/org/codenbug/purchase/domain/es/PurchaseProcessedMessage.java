package org.codenbug.purchase.domain.es;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;

@Entity
@Getter
@Table(name = "purchase_processed_message")
public class PurchaseProcessedMessage {
	@Id
	@Column(name = "message_id", length = 64)
	private String messageId;

	@Column(name = "processed_at", nullable = false)
	private LocalDateTime processedAt;

	protected PurchaseProcessedMessage() {}

	public PurchaseProcessedMessage(String messageId, LocalDateTime processedAt) {
		this.messageId = messageId;
		this.processedAt = processedAt;
	}
}
