package org.codenbug.purchase.domain.event;

public enum PaymentOutboxEventType {
  PAYMENT_CONFIRM_REQUESTED("confirm"),
  SEAT_PURCHASED("seat-purchased"),
  SEAT_PURCHASE_CANCELED("seat-canceled"),
  REFUND_COMPLETED("refund-completed"),
  MANAGER_REFUND_COMPLETED("manager-refund-completed");

  public String value;

  PaymentOutboxEventType(String value) {
    this.value = value;
  }
}
