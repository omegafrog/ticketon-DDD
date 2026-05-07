package org.codenbug.purchase.infra.messaging;

import org.codenbug.purchase.app.event.PurchaseConfirmTransactionCommitted;
import org.codenbug.purchase.domain.es.PurchaseOutboxMessage;
import org.codenbug.purchase.domain.port.es.PurchaseConfirmMessagePublisher;
import org.codenbug.purchase.domain.port.es.PurchaseOutboxStore;

import java.time.LocalDateTime;

import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class PurchaseConfirmTransactionCommittedListener {
  private final PurchaseOutboxStore outboxRepository;
  private final PurchaseConfirmMessagePublisher messagePublisher;

  public PurchaseConfirmTransactionCommittedListener(PurchaseOutboxStore outboxRepository,
      PurchaseConfirmMessagePublisher messagePublisher) {
    this.outboxRepository = outboxRepository;
    this.messagePublisher = messagePublisher;
  }

  @TransactionalEventListener(PurchaseConfirmTransactionCommitted.class)
  public void publishMessage(PurchaseConfirmTransactionCommitted event) {
    if (event.getMessageId() == null || event.getMessageId().isBlank()) {
      return;
    }
    outboxRepository.findByMessageId(event.getMessageId())
        .ifPresent(this::publishAndMark);
  }

  private void publishAndMark(PurchaseOutboxMessage message) {
    if (message.getPublishedAt() != null) {
      return;
    }

    try {
      messagePublisher.publish(message);
    } catch (Exception e) {
      log.warn("payment confirm afterCommit publish failed. messageId={}", message.getMessageId(), e);
      message.markPublishAttemptFailed(e.getMessage());
      outboxRepository.save(message);
      return;
    }

    try {
      message.markPublished(LocalDateTime.now());
      outboxRepository.save(message);
    } catch (Exception e) {
      log.warn("payment confirm afterCommit publishedAt update failed. messageId={}", message.getMessageId(), e);
    }
  }
}
