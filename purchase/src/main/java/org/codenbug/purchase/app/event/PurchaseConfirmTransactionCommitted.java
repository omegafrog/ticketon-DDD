package org.codenbug.purchase.app.event;

import lombok.Getter;

@Getter
public class PurchaseConfirmTransactionCommitted {
  private String messageId;
  private String payloadJson;

  protected PurchaseConfirmTransactionCommitted() {
  }

  public PurchaseConfirmTransactionCommitted(String messageId, String payloadJson) {
    this.messageId = messageId;
    this.payloadJson = payloadJson;
  }

}
