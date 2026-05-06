package org.codenbug.purchase.domain;

import java.util.List;

public class PaymentCancellationInfo {
	private final String paymentKey;
	private final String orderId;
	private final String status;
	private final String method;
	private final Integer totalAmount;
	private final String receiptUrl;
	private final List<CancelDetail> cancels;

	public PaymentCancellationInfo(String paymentKey, String orderId, String status, String method, Integer totalAmount,
			String receiptUrl, List<CancelDetail> cancels) {
		this.paymentKey = paymentKey;
		this.orderId = orderId;
		this.status = status;
		this.method = method;
		this.totalAmount = totalAmount;
		this.receiptUrl = receiptUrl;
		this.cancels = cancels == null ? List.of() : List.copyOf(cancels);
	}

	public String getPaymentKey() {
		return paymentKey;
	}

	public String getOrderId() {
		return orderId;
	}

	public String getStatus() {
		return status;
	}

	public String getMethod() {
		return method;
	}

	public Integer getTotalAmount() {
		return totalAmount;
	}

	public String getReceiptUrl() {
		return receiptUrl;
	}

	public List<CancelDetail> getCancels() {
		return cancels;
	}

	public int getTotalCancelAmount() {
		return cancels.stream().mapToInt(CancelDetail::getCancelAmount).sum();
	}

	public String getFirstCanceledAt() {
		return cancels.isEmpty() ? null : cancels.get(0).getCanceledAt();
	}

	public static class CancelDetail {
		private final Integer cancelAmount;
		private final String canceledAt;
		private final String cancelReason;

		public CancelDetail(Integer cancelAmount, String canceledAt, String cancelReason) {
			this.cancelAmount = cancelAmount;
			this.canceledAt = canceledAt;
			this.cancelReason = cancelReason;
		}

		public Integer getCancelAmount() {
			return cancelAmount;
		}

		public String getCanceledAt() {
			return canceledAt;
		}

		public String getCancelReason() {
			return cancelReason;
		}
	}
}
