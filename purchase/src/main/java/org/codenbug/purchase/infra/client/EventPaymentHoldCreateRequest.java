package org.codenbug.purchase.infra.client;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class EventPaymentHoldCreateRequest {
	private Long expectedSalesVersion;
	private Integer ttlSeconds;
	private String purchaseId;
}
