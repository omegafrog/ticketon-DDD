package org.codenbug.purchase.app;

import org.codenbug.purchase.domain.PaymentCancellationInfo;
import org.codenbug.purchase.domain.PaymentConfirmationInfo;

public interface PGApiService {
  PaymentConfirmationInfo confirmPayment(String paymentKey, String orderId, Integer amount);

  PaymentCancellationInfo cancelPayment(String paymentKey, String cancelReason);

  default PaymentConfirmationInfo confirmPayment(String paymentKey, String orderId, Integer amount,
      String idempotencyKey) {
    return confirmPayment(paymentKey, orderId, amount);
  }

  default PaymentCancellationInfo cancelPayment(String paymentKey, String cancelReason, String idempotencyKey) {
    return cancelPayment(paymentKey, cancelReason);
  }

  boolean supports(PaymentProvider provider);
}
