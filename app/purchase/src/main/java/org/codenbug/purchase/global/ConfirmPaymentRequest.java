package org.codenbug.purchase.global;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ConfirmPaymentRequest {
	private String purchaseId;
	private String paymentKey;
	private String orderId;
	private Integer amount;
}