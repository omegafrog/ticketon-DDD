package org.codenbug.purchase.infra.es;

import java.util.List;

import org.codenbug.purchase.app.es.PaymentOutboxEventType;
import org.codenbug.purchase.app.es.PurchaseOutboxStore;
import org.codenbug.purchase.domain.PurchaseId;
import org.codenbug.purchase.domain.es.PurchaseOutboxMessage;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

@Component
class PurchaseOutboxStoreAdapter implements PurchaseOutboxStore {
  private final JpaPurchaseOutboxRepository repository;

  PurchaseOutboxStoreAdapter(JpaPurchaseOutboxRepository repository) {
    this.repository = repository;
  }

  @Override
  public PurchaseOutboxMessage save(PurchaseOutboxMessage message) {
    return repository.save(message);
  }

  @Override
  public List<PurchaseOutboxMessage> findUnpublishedByQueueName(String queueName, Pageable pageable) {
    return repository.findUnpublishedByQueueName(queueName, pageable);
  }

  // TODO : TEST 작성해야됨
  @Override
  public boolean existsByPurchaseIdAndEventType(PurchaseId purchaseId, PaymentOutboxEventType eventType) {
    return repository.existsByPurchaseIdAndEventType(eventType.value + ":" + purchaseId.getValue());
  }
}
