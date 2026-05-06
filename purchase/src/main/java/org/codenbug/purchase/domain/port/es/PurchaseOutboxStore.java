package org.codenbug.purchase.domain.port.es;

import org.codenbug.purchase.domain.event.PaymentOutboxEventType;
import java.util.List;

import org.codenbug.purchase.domain.PurchaseId;
import org.codenbug.purchase.domain.es.PurchaseOutboxMessage;
import org.springframework.data.domain.Pageable;

public interface PurchaseOutboxStore {
  PurchaseOutboxMessage save(PurchaseOutboxMessage message);

  List<PurchaseOutboxMessage> findUnpublishedByQueueName(String queueName, Pageable pageable);

  boolean existsByPurchaseIdAndEventType(PurchaseId purchaseId, PaymentOutboxEventType eventType);
}
