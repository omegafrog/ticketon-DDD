package org.codenbug.purchase.infra.command.es;

import org.codenbug.purchase.infra.es.JpaPurchaseOutboxRepository;
import java.util.List;

import org.codenbug.purchase.domain.event.PaymentOutboxEventType;
import org.codenbug.purchase.domain.port.es.PurchaseOutboxStore;
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

  @Override
  public boolean existsByPurchaseIdAndEventType(PurchaseId purchaseId, PaymentOutboxEventType eventType) {
    return repository.existsByPurchaseIdAndEventType(PurchaseOutboxMessage.idempotencyKey(purchaseId, eventType));
  }
}
