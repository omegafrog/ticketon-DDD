package org.codenbug.message;

import lombok.Getter;

@Getter
public class PaymentConfirmSuccessEvent {
	public static final String PAYMENT_CONFIRM_SUCCESS_TOPIC = "payment-confirm-success";
	private String purchaseId;
	private String paymentKey;
	private String method;
	private String approvedAt;

	protected PaymentConfirmSuccessEvent(){}
	public PaymentConfirmSuccessEvent(String purchaseId, String paymentKey, String method,
		String approvedAt) {
		this.purchaseId = purchaseId;
		this.paymentKey = paymentKey;
		this.method = method;
		this.approvedAt = approvedAt;
	}
}
