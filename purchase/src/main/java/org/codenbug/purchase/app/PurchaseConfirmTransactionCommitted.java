package org.codenbug.purchase.app;

import lombok.Getter;

@Getter
public class PurchaseConfirmTransactionCommitted {
  private String payloadJson;

  protected PurchaseConfirmTransactionCommitted() {
  }

  public PurchaseConfirmTransactionCommitted(String payloadJson) {
    this.payloadJson = payloadJson;
  }

}
