package org.codenbug.purchase.domain.port.es;

import org.codenbug.purchase.domain.es.PurchaseOutboxMessage;

public interface PurchaseConfirmMessagePublisher {
  void publish(PurchaseOutboxMessage message);
}
