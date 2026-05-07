package org.codenbug.purchase.app.es;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.Optional;

import org.codenbug.purchase.app.command.es.PurchaseConfirmStatusProjectionSerivce;
import org.codenbug.purchase.app.command.es.PurchaseConfirmWorker;
import org.codenbug.purchase.app.command.es.PurchasePaymentFinalizationService;
import org.codenbug.purchase.app.support.PaymentProviderRouter;
import org.codenbug.purchase.domain.EventSummary;
import org.codenbug.purchase.domain.PaymentProvider;
import org.codenbug.purchase.domain.Purchase;
import org.codenbug.purchase.domain.PurchaseId;
import org.codenbug.purchase.domain.UserId;
import org.codenbug.purchase.domain.es.PurchaseConfirmStatus;
import org.codenbug.purchase.domain.port.PGApiService;
import org.codenbug.purchase.domain.port.EventInfoProvider;
import org.codenbug.purchase.domain.port.PurchaseRepository;
import org.codenbug.purchase.domain.port.es.PurchaseProcessedMessageStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;

import com.fasterxml.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
class PurchaseConfirmWorkerTest {

  @Mock
  private PlatformTransactionManager transactionManager;
  @Mock
  private TransactionStatus transactionStatus;
  @Mock
  private PurchaseProcessedMessageStore processedMessageRepository;
  @Mock
  private PurchaseConfirmStatusProjectionSerivce eventAppendService;
  @Mock
  private EventInfoProvider eventServiceClient;
  @Mock
  private PaymentProviderRouter paymentProviderRouter;
  @Mock
  private PGApiService pgApiService;
  @Mock
  private PurchasePaymentFinalizationService finalizationService;
  @Mock
  private PurchaseRepository purchaseRepository;

  private PurchaseConfirmWorker worker;

  @BeforeEach
  void setUp() {
    when(transactionManager.getTransaction(any())).thenReturn(transactionStatus);
    worker = new PurchaseConfirmWorker(
        new ObjectMapper(),
        transactionManager,
        processedMessageRepository,
        eventAppendService,
        eventServiceClient,
        paymentProviderRouter,
        finalizationService,
        purchaseRepository);
  }

  @Test
  void process_whenEventVersionChanges_rejectsBeforePgCall() {
    Purchase purchase = new Purchase("e1", "order1", 1000, 1L, new UserId("u1"));
    when(purchaseRepository.findById(any(PurchaseId.class))).thenReturn(Optional.of(purchase));
    when(eventServiceClient.getEventSummary("e1")).thenReturn(
        new EventSummary("e1", 1L, true, "OPEN", 2L, 2L, "event-title"));

    worker.process("msg-1",
        "{\"purchaseId\":\"%s\",\"userId\":\"u1\",\"eventId\":\"e1\",\"expectedSalesVersion\":1,\"paymentKey\":\"payKey\",\"orderId\":\"order1\",\"amount\":1000,\"provider\":\"TOSS\"}"
            .formatted(purchase.getPurchaseId().getValue()));

    verify(eventAppendService).upadteProjectionStatus(any(PurchaseId.class), anyMap(),
        eq(PurchaseConfirmStatus.PROCESSING), eq("processing"));
    verify(eventAppendService).upadteProjectionStatus(any(PurchaseId.class), anyMap(),
        eq(PurchaseConfirmStatus.REJECTED), eq("event changed; payment returned to pending"));
    verifyNoInteractions(paymentProviderRouter, finalizationService);
  }

  @Test
  void process_whenPgConfirmFails_releasesMarkerSoSameMessageCanRetry() {
    Purchase purchase = new Purchase("e1", "order1", 1000, 1L, new UserId("u1"));
    String payloadJson = "{\"purchaseId\":\"%s\",\"userId\":\"u1\",\"eventId\":\"e1\",\"eventType\":\"PAYMENT_CONFIRM_REQUESTED\",\"expectedSalesVersion\":1,\"paymentKey\":\"payKey\",\"orderId\":\"order1\",\"amount\":1000,\"provider\":\"TOSS\"}"
        .formatted(purchase.getPurchaseId().getValue());

    when(purchaseRepository.findById(any(PurchaseId.class))).thenReturn(Optional.of(purchase));
    when(eventServiceClient.getEventSummary("e1")).thenReturn(
        new EventSummary("e1", 1L, true, "OPEN", 1L, 1L, "event-title"));
    when(paymentProviderRouter.get(PaymentProvider.TOSS)).thenReturn(pgApiService);
    when(pgApiService.confirmPayment("payKey", "order1", 1000, "confirm:" + purchase.getPurchaseId().getValue()))
        .thenThrow(new IllegalStateException("pg unavailable"));

    org.assertj.core.api.Assertions.assertThatThrownBy(() -> worker.process("confirm:" + purchase.getPurchaseId().getValue(), payloadJson))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("pg unavailable");
    org.assertj.core.api.Assertions.assertThatThrownBy(() -> worker.process("confirm:" + purchase.getPurchaseId().getValue(), payloadJson))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("pg unavailable");

    verify(processedMessageRepository, times(2)).save(any());
    verify(processedMessageRepository, times(2)).deleteById("confirm:" + purchase.getPurchaseId().getValue());
    verify(pgApiService, times(2)).confirmPayment("payKey", "order1", 1000,
        "confirm:" + purchase.getPurchaseId().getValue());
  }
}
