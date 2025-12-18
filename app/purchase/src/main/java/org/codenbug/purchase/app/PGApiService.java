package org.codenbug.purchase.app;

import org.codenbug.purchase.infra.CanceledPaymentInfo;
import org.codenbug.purchase.infra.ConfirmedPaymentInfo;

public interface PGApiService {
	ConfirmedPaymentInfo confirmPayment(String paymentKey, String orderId, Integer amount);

	CanceledPaymentInfo cancelPayment(String paymentKey, String cancelReason);
}
