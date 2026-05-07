package org.codenbug.purchase.app.es;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

import org.codenbug.purchase.app.command.es.PurchaseConfirmWorker;
import org.codenbug.purchase.app.event.PaymentConfirmedConsumer;
import org.codenbug.purchase.domain.PurchaseId;
import org.codenbug.purchase.domain.event.PaymentOutboxEventType;
import org.codenbug.purchase.domain.es.PurchaseOutboxMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fasterxml.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
class PaymentConfirmedConsumerTest {

  @Mock
  private PurchaseConfirmWorker confirmWorker;

  private PaymentConfirmedConsumer consumer;

  @BeforeEach
  void setUp() {
    consumer = new PaymentConfirmedConsumer(confirmWorker, new ObjectMapper());
  }

  @Test
  void handle_whenMessageBlank_rejectsWithoutProcessing() {
    assertThatThrownBy(() -> consumer.handle(" "))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("blank payment confirm message");

    verifyNoInteractions(confirmWorker);
  }

  @Test
  void handle_whenPurchaseIdMissing_rejectsWithoutProcessing() {
    String json = "{\"eventType\":\"PAYMENT_CONFIRM_REQUESTED\"}";

    assertThatThrownBy(() -> consumer.handle(json))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("missing purchaseId");

    verifyNoInteractions(confirmWorker);
  }

  @Test
  void handle_whenEventTypeInvalid_rejectsWithoutProcessing() {
    String json = "{\"purchaseId\":\"p1\",\"eventType\":\"UNKNOWN\"}";

    assertThatThrownBy(() -> consumer.handle(json))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("invalid payment confirm eventType");

    verifyNoInteractions(confirmWorker);
  }

  @Test
  void handle_whenValidConfirmRequested_usesConfirmCommandId() {
    String json = "{\"purchaseId\":\"p1\",\"eventType\":\"PAYMENT_CONFIRM_REQUESTED\"}";

    consumer.handle(json);

    verify(confirmWorker).process("confirm:p1", json);
  }

  @Test
  void handle_whenPublishedOutboxPayloadConsumed_usesSameIdempotencyKeyAsOutbox() {
    String json = "{\"purchaseId\":\"p1\",\"eventType\":\"PAYMENT_CONFIRM_REQUESTED\"}";
    String outboxMessageId = PurchaseOutboxMessage.messageIdFor(
        PaymentOutboxEventType.PAYMENT_CONFIRM_REQUESTED,
        new PurchaseId("p1"));

    consumer.handle(json);

    verify(confirmWorker).process(outboxMessageId, json);
  }
}
