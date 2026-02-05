package org.codenbug.event.global;

import lombok.Getter;

@Getter
public class EventPaymentHoldCreateRequest {
	private Long expectedSalesVersion;
	private Integer ttlSeconds;
	private String purchaseId;

	protected EventPaymentHoldCreateRequest() {}

	public Long getExpectedSalesVersion() {
		return expectedSalesVersion;
	}

	public Integer getTtlSeconds() {
		return ttlSeconds;
	}

	public String getPurchaseId() {
		return purchaseId;
	}
}
