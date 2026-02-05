package org.codenbug.purchase.global;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ConfirmPaymentAcceptedResponse {
	private String purchaseId;
	private String status;
	private String statusUrl;
}
