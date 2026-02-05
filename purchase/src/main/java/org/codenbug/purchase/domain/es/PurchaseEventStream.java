package org.codenbug.purchase.domain.es;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Getter;

@Entity
@Getter
@Table(name = "purchase_event_stream")
public class PurchaseEventStream {
	@Id
	@Column(name = "purchase_id", length = 64)
	private String purchaseId;

	@Column(name = "last_seq", nullable = false)
	private long lastSeq;

	@Version
	private long version;

	protected PurchaseEventStream() {}

	public PurchaseEventStream(String purchaseId) {
		this.purchaseId = purchaseId;
		this.lastSeq = 0L;
	}

	public long nextSeq() {
		this.lastSeq++;
		return this.lastSeq;
	}
}
