package org.codenbug.purchase.app.command.es;

import org.codenbug.purchase.domain.port.es.PurchaseConfirmStatusProjectionStore;
import org.codenbug.purchase.domain.port.es.PurchaseConfirmMessagePublisher;
import org.codenbug.purchase.domain.port.es.PurchaseOutboxStore;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.codenbug.purchase.domain.PurchaseId;
import org.codenbug.purchase.domain.es.PurchaseConfirmStatus;
import org.codenbug.purchase.domain.es.PurchaseOutboxMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class PurchaseConfirmScheduler {
  private static final int BATCH_SIZE = 50;

  private final ObjectMapper objectMapper;
  private final PurchaseOutboxStore outboxRepository;
  private final PurchaseConfirmStatusProjectionStore statusProjectionRepository;
  private final PurchaseConfirmMessagePublisher messagePublisher;
  private final int maxPublishAttempts;

  public PurchaseConfirmScheduler(ObjectMapper objectMapper,
      PurchaseOutboxStore outboxRepository,
      PurchaseConfirmStatusProjectionStore statusProjectionRepository,
      PurchaseConfirmMessagePublisher messagePublisher,
      @Value("${purchase.outbox.max-publish-attempts:5}") int maxPublishAttempts) {
    this.objectMapper = objectMapper;
    this.outboxRepository = outboxRepository;
    this.statusProjectionRepository = statusProjectionRepository;
    this.messagePublisher = messagePublisher;
    this.maxPublishAttempts = maxPublishAttempts;
  }

  @Scheduled(fixedDelayString = "${purchase.outbox.publish-interval-ms:500}")
  public void processPendingConfirms() {
    List<PurchaseOutboxMessage> batch = outboxRepository.findUnpublishedByQueueName(
        PurchaseConfirmCommandService.CONFIRM_WORK_QUEUE,
        PageRequest.of(0, BATCH_SIZE));
    if (batch.isEmpty()) {
      return;
    }

    for (PurchaseOutboxMessage msg : batch) {
      String purchaseIdValue = extractPurchaseId(msg.getPayloadJson());
      if (purchaseIdValue == null || purchaseIdValue.isBlank()) {
        msg.markFailedPermanently(LocalDateTime.now(), "invalid outbox payload: missing purchaseId");
        outboxRepository.save(msg);
        continue;
      }

      PurchaseId purchaseId = new PurchaseId(purchaseIdValue);
      PurchaseConfirmStatus currentStatus = findCurrentStatus(purchaseId);

      if (isTerminalStatus(currentStatus)) {
        msg.markFailedPermanently(LocalDateTime.now(), "non-retryable status: " + currentStatus.name());
        outboxRepository.save(msg);
        continue;
      }

      if (msg.getPublishAttempts() >= maxPublishAttempts) {
        msg.markFailedPermanently(LocalDateTime.now(), "max publish attempts exceeded");
        statusProjectionRepository.findById(purchaseId)
            .ifPresent(projection -> {
              projection.update(PurchaseConfirmStatus.FAILED, "max attempts exceeded", LocalDateTime.now());
              statusProjectionRepository.save(projection);
            });
        outboxRepository.save(msg);
        continue;
      }

      try {
        messagePublisher.publish(msg);
        msg.markPublished(LocalDateTime.now());
        outboxRepository.save(msg);
      } catch (Exception e) {
        log.info(e.getMessage(), e);
        msg.markPublishAttemptFailed(e.getMessage());
        outboxRepository.save(msg);
      }
    }
  }

  private PurchaseConfirmStatus findCurrentStatus(PurchaseId purchaseId) {
    if (purchaseId == null || purchaseId.getValue().isBlank()) {
      return null;
    }
    return statusProjectionRepository.findById(purchaseId)
        .map(projection -> projection.getStatus())
        .orElse(null);
  }

  private boolean isTerminalStatus(PurchaseConfirmStatus status) {
    if (status == null) {
      return false;
    }
    return status == PurchaseConfirmStatus.DONE
        || status == PurchaseConfirmStatus.FAILED
        || status == PurchaseConfirmStatus.REJECTED;
  }

  private String extractPurchaseId(String payloadJson) {
    if (payloadJson == null || payloadJson.isBlank()) {
      return null;
    }
    try {
      @SuppressWarnings("unchecked")
      Map<String, Object> map = objectMapper.readValue(payloadJson, Map.class);
      Object value = map.get("purchaseId");
      return value == null ? null : value.toString();
    } catch (Exception ignore) {
      return null;
    }
  }
}
