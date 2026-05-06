package org.codenbug.purchase.domain;

public class PaymentConfirmationInfo {
	private final String paymentKey;
	private final String orderId;
	private final String orderName;
	private final Integer totalAmount;
	private final String status;
	private final String method;
	private final String approvedAt;
	private final String receiptUrl;

	public PaymentConfirmationInfo(String paymentKey, String orderId, String orderName, Integer totalAmount,
			String status, String method, String approvedAt, String receiptUrl) {
		this.paymentKey = paymentKey;
		this.orderId = orderId;
		this.orderName = orderName;
		this.totalAmount = totalAmount;
		this.status = status;
		this.method = method;
		this.approvedAt = approvedAt;
		this.receiptUrl = receiptUrl;
	}

	public String getPaymentKey() {
		return paymentKey;
	}

	public String getOrderId() {
		return orderId;
	}

	public String getOrderName() {
		return orderName;
	}

	public Integer getTotalAmount() {
		return totalAmount;
	}

	public String getStatus() {
		return status;
	}

	public String getMethod() {
		return method;
	}

	public String getApprovedAt() {
		return approvedAt;
	}

	public String getReceiptUrl() {
		return receiptUrl;
	}
}
