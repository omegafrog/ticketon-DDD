package org.codenbug.purchase.ui.response;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class InitiatePaymentResponse {
  private String purchaseId;
  private String status;
  private LocalDateTime paymentDeadlineAt;
  private String orderId;

  public InitiatePaymentResponse(String purchaseId, String orderId, String status) {
    this(purchaseId, status, null, orderId);
  }
}
