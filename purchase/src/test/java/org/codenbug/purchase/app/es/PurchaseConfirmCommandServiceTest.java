package org.codenbug.purchase.app.es;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.Optional;

import org.codenbug.purchase.app.command.es.PurchaseConfirmCommandService;
import org.codenbug.purchase.app.event.PurchaseConfirmTransactionCommitted;
import org.codenbug.purchase.domain.Purchase;
import org.codenbug.purchase.domain.PurchaseId;
import org.codenbug.purchase.domain.event.PaymentOutboxEventType;
import org.codenbug.purchase.domain.es.PurchaseOutboxMessage;
import org.codenbug.purchase.domain.port.PurchaseRepository;
import org.codenbug.purchase.domain.port.es.PurchaseConfirmStatusProjectionStore;
import org.codenbug.purchase.domain.port.es.PurchaseOutboxStore;
import org.codenbug.purchase.ui.request.ConfirmPaymentRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import com.fasterxml.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
class PurchaseConfirmCommandServiceTest {
  @Spy
  private ObjectMapper objectMapper = new ObjectMapper();

  @Mock
  private PurchaseRepository purchaseRepository;
  @Mock
  private PurchaseOutboxStore outboxRepository;
  @Mock
  private PurchaseConfirmStatusProjectionStore statusProjectionRepository;
  @Mock
  private ApplicationEventPublisher eventPublisher;

  @InjectMocks
  private PurchaseConfirmCommandService service;

  @Test
  void requestConfirm_whenAlreadyRequested_doesNotEnqueueAgain() {
    ConfirmPaymentRequest req = new ConfirmPaymentRequest("p1", "payKey", "order1", 1000, "TOSS");
    Purchase purchase = mock(Purchase.class);

    when(purchaseRepository.findById(any(PurchaseId.class))).thenReturn(Optional.of(purchase));
    when(outboxRepository.existsByPurchaseIdAndEventType(any(PurchaseId.class),
        eq(PaymentOutboxEventType.PAYMENT_CONFIRM_REQUESTED))).thenReturn(true);

    service.requestConfirm(req, "u1");

    verify(outboxRepository, never()).save(any());
    verify(statusProjectionRepository, never()).save(any());
    verify(eventPublisher, never()).publishEvent(any(Object.class));
  }

  @Test
  void requestConfirm_whenFirstRequested_updatesProjectionAndEnqueues() {
    ConfirmPaymentRequest req = new ConfirmPaymentRequest("p1", "payKey", "order1", 1000, "TOSS");
    Purchase purchase = mock(Purchase.class);

    when(purchaseRepository.findById(any(PurchaseId.class))).thenReturn(Optional.of(purchase));
    when(purchase.getExpectedSalesVersion()).thenReturn(1L);
    when(purchase.getEventId()).thenReturn("e1");
    when(outboxRepository.existsByPurchaseIdAndEventType(any(PurchaseId.class),
        eq(PaymentOutboxEventType.PAYMENT_CONFIRM_REQUESTED))).thenReturn(false);
    when(statusProjectionRepository.findById(any(PurchaseId.class))).thenReturn(Optional.empty());
    when(outboxRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

    service.requestConfirm(req, "u1");

    verify(statusProjectionRepository).save(any());
    ArgumentCaptor<PurchaseOutboxMessage> messageCaptor = ArgumentCaptor.forClass(PurchaseOutboxMessage.class);
    verify(outboxRepository).save(messageCaptor.capture());
    org.assertj.core.api.Assertions.assertThat(messageCaptor.getValue().getMessageId()).isEqualTo("confirm:p1");
    ArgumentCaptor<Object> eventCaptor = ArgumentCaptor.forClass(Object.class);
    verify(eventPublisher).publishEvent(eventCaptor.capture());
    PurchaseConfirmTransactionCommitted event = (PurchaseConfirmTransactionCommitted) eventCaptor.getValue();
    org.assertj.core.api.Assertions.assertThat(event.getMessageId()).isEqualTo("confirm:p1");
    org.assertj.core.api.Assertions.assertThat(event.getPayloadJson()).contains("\"purchaseId\":\"p1\"");
  }

  @Test
  void requestConfirm_whenPurchaseVersionMissing_throws() {
    ConfirmPaymentRequest req = new ConfirmPaymentRequest("p1", "payKey", "order1", 1000, "TOSS");
    Purchase purchase = mock(Purchase.class);

    when(purchaseRepository.findById(any(PurchaseId.class))).thenReturn(Optional.of(purchase));
    when(purchase.getExpectedSalesVersion()).thenReturn(null);
    when(outboxRepository.existsByPurchaseIdAndEventType(any(PurchaseId.class),
        eq(PaymentOutboxEventType.PAYMENT_CONFIRM_REQUESTED))).thenReturn(false);

    org.junit.jupiter.api.Assertions.assertThrows(IllegalStateException.class,
        () -> service.requestConfirm(req, "u1"));

    verify(statusProjectionRepository, never()).save(any());
    verify(outboxRepository, never()).save(any());
  }
}
