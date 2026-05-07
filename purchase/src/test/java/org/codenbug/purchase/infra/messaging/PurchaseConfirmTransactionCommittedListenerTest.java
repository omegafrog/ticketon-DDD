package org.codenbug.purchase.infra.messaging;

import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.Optional;

import org.codenbug.purchase.app.command.es.PurchaseConfirmCommandService;
import org.codenbug.purchase.app.event.PurchaseConfirmTransactionCommitted;
import org.codenbug.purchase.domain.es.PurchaseOutboxMessage;
import org.codenbug.purchase.domain.event.PaymentOutboxEventType;
import org.codenbug.purchase.domain.port.es.PurchaseConfirmMessagePublisher;
import org.codenbug.purchase.domain.port.es.PurchaseOutboxStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PurchaseConfirmTransactionCommittedListenerTest {

  @Mock
  private PurchaseOutboxStore outboxRepository;
  @Mock
  private PurchaseConfirmMessagePublisher messagePublisher;

  private PurchaseConfirmTransactionCommittedListener listener;

  @BeforeEach
  void setUp() {
    listener = new PurchaseConfirmTransactionCommittedListener(outboxRepository, messagePublisher);
  }

  @Test
  void publishMessage_whenMessageIdBlank_ignoresEvent() {
    listener.publishMessage(new PurchaseConfirmTransactionCommitted(" ", null));

    verifyNoInteractions(outboxRepository, messagePublisher);
  }

  @Test
  void publishMessage_loadsPayloadFromOutboxInsteadOfEventPayload() {
    PurchaseOutboxMessage message = mock(PurchaseOutboxMessage.class);
    when(outboxRepository.findByMessageId("confirm:p1")).thenReturn(Optional.of(message));

    listener.publishMessage(new PurchaseConfirmTransactionCommitted("confirm:p1", null));

    verify(messagePublisher).publish(message);
    verify(message).markPublished(any());
    verify(outboxRepository).save(message);
  }

  @Test
  void publishMessage_whenOutboxAlreadyPublished_doesNotRepublish() {
    PurchaseOutboxMessage message = PurchaseOutboxMessage.of(
        "confirm:p1",
        PurchaseConfirmCommandService.CONFIRM_WORK_QUEUE,
        PaymentOutboxEventType.PAYMENT_CONFIRM_REQUESTED,
        "{\"purchaseId\":\"p1\",\"eventType\":\"PAYMENT_CONFIRM_REQUESTED\"}",
        LocalDateTime.now());
    message.markPublished(LocalDateTime.now());
    when(outboxRepository.findByMessageId("confirm:p1")).thenReturn(Optional.of(message));

    listener.publishMessage(new PurchaseConfirmTransactionCommitted("confirm:p1", null));

    verifyNoInteractions(messagePublisher);
    verify(outboxRepository, never()).save(any());
  }
}
