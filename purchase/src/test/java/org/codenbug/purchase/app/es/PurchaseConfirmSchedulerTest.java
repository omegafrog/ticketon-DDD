package org.codenbug.purchase.app.es;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.codenbug.purchase.app.command.es.PurchaseConfirmCommandService;
import org.codenbug.purchase.app.command.es.PurchaseConfirmScheduler;
import org.codenbug.purchase.domain.PurchaseId;
import org.codenbug.purchase.domain.es.PurchaseConfirmStatus;
import org.codenbug.purchase.domain.es.PurchaseConfirmStatusProjection;
import org.codenbug.purchase.domain.es.PurchaseOutboxMessage;
import org.codenbug.purchase.domain.event.PaymentOutboxEventType;
import org.codenbug.purchase.domain.port.es.PurchaseConfirmMessagePublisher;
import org.codenbug.purchase.domain.port.es.PurchaseConfirmStatusProjectionStore;
import org.codenbug.purchase.domain.port.es.PurchaseOutboxStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fasterxml.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
class PurchaseConfirmSchedulerTest {

  @Mock
  private PurchaseOutboxStore outboxRepository;
  @Mock
  private PurchaseConfirmStatusProjectionStore statusProjectionRepository;
  @Mock
  private PurchaseConfirmMessagePublisher messagePublisher;

  private PurchaseConfirmScheduler scheduler;

  @BeforeEach
  void setUp() {
    scheduler = new PurchaseConfirmScheduler(
        new ObjectMapper(),
        outboxRepository,
        statusProjectionRepository,
        messagePublisher,
        3);
  }

  @ParameterizedTest
  @EnumSource(value = PurchaseConfirmStatus.class, names = {"PENDING", "PROCESSING"})
  void processPendingConfirms_retriesForRetryableStatuses(PurchaseConfirmStatus status) {
    PurchaseOutboxMessage message = outbox("purchase-retryable");
    when(outboxRepository.findUnpublishedByQueueName(eq(PurchaseConfirmCommandService.CONFIRM_WORK_QUEUE), any()))
        .thenReturn(List.of(message));
    when(statusProjectionRepository.findById(any(PurchaseId.class)))
        .thenReturn(Optional.of(projection("purchase-retryable", status)));

    scheduler.processPendingConfirms();

    verify(messagePublisher).publish(eq(message));
    verify(outboxRepository, atLeastOnce()).save(eq(message));
    assertThat(message.getPublishedAt()).isNotNull();
    assertThat(message.getLastError()).isNull();
  }

  @ParameterizedTest
  @EnumSource(value = PurchaseConfirmStatus.class, names = {"DONE", "FAILED", "REJECTED"})
  void processPendingConfirms_skipsTerminalStatuses(PurchaseConfirmStatus terminalStatus) {
    PurchaseOutboxMessage message = outbox("purchase-terminal");
    when(outboxRepository.findUnpublishedByQueueName(eq(PurchaseConfirmCommandService.CONFIRM_WORK_QUEUE), any()))
        .thenReturn(List.of(message));
    when(statusProjectionRepository.findById(any(PurchaseId.class)))
        .thenReturn(Optional.of(projection("purchase-terminal", terminalStatus)));

    scheduler.processPendingConfirms();

    verify(messagePublisher, never()).publish(any());
    verify(outboxRepository, atLeastOnce()).save(eq(message));
    assertThat(message.getPublishedAt()).isNotNull();
    assertThat(message.getLastError()).contains("non-retryable status");
  }

  @Test
  void processPendingConfirms_marksFailedWhenMaxAttemptsExceeded() {
    PurchaseOutboxMessage message = outbox("purchase-max-attempt");
    message.markPublishAttemptFailed("e1");
    message.markPublishAttemptFailed("e2");
    message.markPublishAttemptFailed("e3");
    PurchaseConfirmStatusProjection projection = projection("purchase-max-attempt", PurchaseConfirmStatus.PENDING);

    when(outboxRepository.findUnpublishedByQueueName(eq(PurchaseConfirmCommandService.CONFIRM_WORK_QUEUE), any()))
        .thenReturn(List.of(message));
    when(statusProjectionRepository.findById(any(PurchaseId.class))).thenReturn(Optional.of(projection));

    scheduler.processPendingConfirms();

    verify(messagePublisher, never()).publish(any());
    verify(statusProjectionRepository).save(projection);
    assertThat(projection.getStatus()).isEqualTo(PurchaseConfirmStatus.FAILED);
    assertThat(message.getPublishedAt()).isNotNull();
    assertThat(message.getLastError()).contains("max publish attempts exceeded");
  }

  @Test
  void processPendingConfirms_keepsOutboxUnpublishedWhenPublishThrows() {
    PurchaseOutboxMessage message = outbox("purchase-throw");
    when(outboxRepository.findUnpublishedByQueueName(eq(PurchaseConfirmCommandService.CONFIRM_WORK_QUEUE), any()))
        .thenReturn(List.of(message));
    when(statusProjectionRepository.findById(any(PurchaseId.class)))
        .thenReturn(Optional.of(projection("purchase-throw", PurchaseConfirmStatus.PENDING)));
    doThrow(new IllegalStateException("boom")).when(messagePublisher).publish(any());

    scheduler.processPendingConfirms();

    verify(messagePublisher).publish(eq(message));
    assertThat(message.getPublishedAt()).isNull();
    assertThat(message.getPublishAttempts()).isEqualTo(1);
    assertThat(message.getLastError()).contains("boom");
  }

  private PurchaseOutboxMessage outbox(String purchaseId) {
    return PurchaseOutboxMessage.of(
        "msg-" + purchaseId,
        PurchaseConfirmCommandService.CONFIRM_WORK_QUEUE,
        PaymentOutboxEventType.PAYMENT_CONFIRM_REQUESTED,
        "{\"purchaseId\":\"" + purchaseId + "\"}",
        LocalDateTime.now());
  }

  private PurchaseConfirmStatusProjection projection(String purchaseId, PurchaseConfirmStatus status) {
    return new PurchaseConfirmStatusProjection(new PurchaseId(purchaseId), status, "test", LocalDateTime.now());
  }
}
