package org.codenbug.purchase.ui.response;

import java.time.LocalDateTime;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class InitiatePaymentResponse {
  private String purchaseId;
  private String status;
  private LocalDateTime paymentDeadlineAt;
  private String orderId;

  private InitiatePaymentResponse(String purchaseId, String status, LocalDateTime paymentDeadlineAt, String orderId) {
    this.purchaseId = purchaseId;
    this.status = status;
    this.paymentDeadlineAt = paymentDeadlineAt;
    this.orderId = orderId;
  }

  public static InitiatePaymentResponse initiated(String purchaseId, String status, LocalDateTime paymentDeadlineAt,
      String orderId) {
    return new InitiatePaymentResponse(purchaseId, status, paymentDeadlineAt, orderId);
  }
}
