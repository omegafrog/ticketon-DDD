package org.codenbug.purchase.app;

import org.codenbug.purchase.infra.CanceledPaymentInfo;
import org.codenbug.purchase.infra.ConfirmedPaymentInfo;

public interface PGApiService {
	ConfirmedPaymentInfo confirmPayment(String paymentKey, String orderId, Integer amount);

	CanceledPaymentInfo cancelPayment(String paymentKey, String cancelReason);

	default ConfirmedPaymentInfo confirmPayment(String paymentKey, String orderId, Integer amount, String idempotencyKey) {
		return confirmPayment(paymentKey, orderId, amount);
	}

	default CanceledPaymentInfo cancelPayment(String paymentKey, String cancelReason, String idempotencyKey) {
		return cancelPayment(paymentKey, cancelReason);
	}

	boolean supports(PaymentProvider provider);
}
