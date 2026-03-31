package org.codenbug.event.global;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

@Getter
public class EventPaymentHoldCreateRequest {
	@NotNull
	@Min(0)
	private Long expectedSalesVersion;

	@NotNull
	@Min(1)
	private Integer ttlSeconds;

	@NotBlank
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
