package org.codenbug.purchase.infra.messaging;

import static org.mockito.Mockito.*;

import java.util.Optional;

import org.codenbug.purchase.app.event.PurchaseConfirmTransactionCommitted;
import org.codenbug.purchase.domain.es.PurchaseOutboxMessage;
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
}
