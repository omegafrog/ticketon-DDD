package org.codenbug.purchase.app.es;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.codenbug.purchase.app.command.es.PurchaseConfirmStatusProjectionSerivce;
import org.codenbug.purchase.app.command.es.PurchaseConfirmWorker;
import org.codenbug.purchase.app.command.es.PurchasePaymentFinalizationService;
import org.codenbug.purchase.app.support.PaymentProviderRouter;
import org.codenbug.purchase.domain.EventSummary;
import org.codenbug.purchase.domain.PaymentCancellationInfo;
import org.codenbug.purchase.domain.PaymentConfirmationInfo;
import org.codenbug.purchase.domain.PaymentProvider;
import org.codenbug.purchase.domain.PaymentStatus;
import org.codenbug.purchase.domain.Purchase;
import org.codenbug.purchase.domain.PurchaseId;
import org.codenbug.purchase.domain.Refund;
import org.codenbug.purchase.domain.RefundStatus;
import org.codenbug.purchase.domain.UserId;
import org.codenbug.purchase.domain.es.PurchaseConfirmStatus;
import org.codenbug.purchase.domain.port.EventInfoProvider;
import org.codenbug.purchase.domain.port.PGApiService;
import org.codenbug.purchase.domain.port.PurchaseRepository;
import org.codenbug.purchase.domain.port.RefundRepository;
import org.codenbug.purchase.domain.port.es.PurchaseProcessedMessageStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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
  @Mock
  private RefundRepository refundRepository;

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
        purchaseRepository,
        refundRepository);
  }

  @Test
  void process_whenEventVersionChanges_rejectsBeforePgCall() {
    Purchase purchase = new Purchase("e1", "order1", 1000, 1L, new UserId("u1"));
    when(purchaseRepository.findById(any(PurchaseId.class))).thenReturn(Optional.of(purchase));
    when(eventServiceClient.getEventSummary("e1")).thenReturn(
        new EventSummary("e1", 1L, true, "OPEN", 2L, 2L, "event-title"));

    worker.process("msg-1", payloadFor(purchase));

    verify(eventAppendService).upadteProjectionStatus(any(PurchaseId.class), anyMap(),
        eq(PurchaseConfirmStatus.PROCESSING), eq("processing"));
    verify(eventAppendService).upadteProjectionStatus(any(PurchaseId.class), anyMap(),
        eq(PurchaseConfirmStatus.REJECTED), eq("event changed; payment returned to pending"));
    verifyNoInteractions(paymentProviderRouter, finalizationService);
  }

  @Test
  void process_whenPgConfirmFailsBeforeApproval_doesNotCompensateAndReleasesMarker() {
    Purchase purchase = new Purchase("e1", "order1", 1000, 1L, new UserId("u1"));
    String messageId = "confirm:" + purchase.getPurchaseId().getValue();

    when(purchaseRepository.findById(any(PurchaseId.class))).thenReturn(Optional.of(purchase));
    when(eventServiceClient.getEventSummary("e1")).thenReturn(eventSummary("e1", 1L));
    when(paymentProviderRouter.get(PaymentProvider.TOSS)).thenReturn(pgApiService);
    when(pgApiService.confirmPayment("payKey", "order1", 1000, messageId))
        .thenThrow(new IllegalStateException("pg unavailable"));

    assertThatThrownBy(() -> worker.process(messageId, payloadFor(purchase)))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("pg unavailable");

    verify(pgApiService, never()).cancelPayment(anyString(), anyString(), anyString());
    verifyNoInteractions(refundRepository);
    verify(processedMessageRepository).deleteById(messageId);
    verify(eventAppendService).upadteProjectionStatus(any(PurchaseId.class), anyMap(),
        eq(PurchaseConfirmStatus.FAILED), eq("pg confirm failed"));
  }

  @Test
  void process_whenFinalizationFailsAfterPgConfirm_compensatesWithPgCancel() {
    Purchase purchase = new Purchase("e1", "order1", 1000, 1L, new UserId("u1"));
    PaymentConfirmationInfo paymentInfo = paymentInfo(purchase);
    PaymentCancellationInfo cancellationInfo = cancellationInfo(purchase);

    when(purchaseRepository.findById(any(PurchaseId.class))).thenReturn(Optional.of(purchase));
    when(eventServiceClient.getEventSummary("e1")).thenReturn(eventSummary("e1", 1L));
    when(paymentProviderRouter.get(PaymentProvider.TOSS)).thenReturn(pgApiService);
    when(pgApiService.confirmPayment("payKey", "order1", 1000, "confirm:" + purchase.getPurchaseId().getValue()))
        .thenReturn(paymentInfo);
    when(pgApiService.cancelPayment("payKey", "Local payment finalization failed after PG confirm",
        "compensate:" + purchase.getPurchaseId().getValue())).thenReturn(cancellationInfo);
    when(finalizationService.finalizePayment(any(PurchaseId.class), eq(paymentInfo), eq("u1")))
        .thenThrow(new IllegalStateException("ticket save failed"));

    worker.process("msg-finalization-fail", payloadFor(purchase));

    verify(pgApiService).cancelPayment("payKey", "Local payment finalization failed after PG confirm",
        "compensate:" + purchase.getPurchaseId().getValue());
    assertThat(purchase.getPaymentStatus()).isEqualTo(PaymentStatus.FAILED);

    ArgumentCaptor<Refund> refundCaptor = ArgumentCaptor.forClass(Refund.class);
    verify(refundRepository).save(refundCaptor.capture());
    assertThat(refundCaptor.getValue().getStatus()).isEqualTo(RefundStatus.COMPLETED);
    assertThat(refundCaptor.getValue().getPgTransactionId()).isEqualTo("payKey");

    verify(eventAppendService).upadteProjectionStatus(any(PurchaseId.class), anyMap(),
        eq(PurchaseConfirmStatus.FAILED), eq("pg confirm compensated after finalization failure"));
  }

  @Test
  void process_whenFinalizationFailsAndCompensationFails_recordsManualState() {
    Purchase purchase = new Purchase("e1", "order1", 1000, 1L, new UserId("u1"));
    PaymentConfirmationInfo paymentInfo = paymentInfo(purchase);

    when(purchaseRepository.findById(any(PurchaseId.class))).thenReturn(Optional.of(purchase));
    when(eventServiceClient.getEventSummary("e1")).thenReturn(eventSummary("e1", 1L));
    when(paymentProviderRouter.get(PaymentProvider.TOSS)).thenReturn(pgApiService);
    when(pgApiService.confirmPayment("payKey", "order1", 1000, "confirm:" + purchase.getPurchaseId().getValue()))
        .thenReturn(paymentInfo);
    when(pgApiService.cancelPayment("payKey", "Local payment finalization failed after PG confirm",
        "compensate:" + purchase.getPurchaseId().getValue())).thenThrow(new RuntimeException("cancel failed"));
    when(finalizationService.finalizePayment(any(PurchaseId.class), eq(paymentInfo), eq("u1")))
        .thenThrow(new IllegalStateException("ticket save failed"));

    worker.process("msg-compensation-fail", payloadFor(purchase));

    ArgumentCaptor<Refund> refundCaptor = ArgumentCaptor.forClass(Refund.class);
    verify(refundRepository).save(refundCaptor.capture());
    assertThat(refundCaptor.getValue().getStatus()).isEqualTo(RefundStatus.FAILED);
    assertThat(purchase.getPaymentStatus()).isEqualTo(PaymentStatus.IN_PROGRESS);
    verify(eventAppendService).upadteProjectionStatus(any(PurchaseId.class), anyMap(),
        eq(PurchaseConfirmStatus.COMPENSATION_REQUIRED), eq("pg confirm succeeded; compensation required"));
  }

  private EventSummary eventSummary(String eventId, Long salesVersion) {
    return new EventSummary(eventId, 1L, true, "OPEN", 1L, salesVersion, "event-title");
  }

  private String payloadFor(Purchase purchase) {
    return "{\"purchaseId\":\"%s\",\"userId\":\"u1\",\"eventId\":\"e1\",\"expectedSalesVersion\":1,\"paymentKey\":\"payKey\",\"orderId\":\"order1\",\"amount\":1000,\"provider\":\"TOSS\"}"
        .formatted(purchase.getPurchaseId().getValue());
  }

  private PaymentConfirmationInfo paymentInfo(Purchase purchase) {
    return new PaymentConfirmationInfo("payKey", purchase.getOrderId(), "test order", purchase.getAmount(),
        "DONE", "CARD", "2026-03-26T08:00:00+09:00", "https://receipt.example.com");
  }

  private PaymentCancellationInfo cancellationInfo(Purchase purchase) {
    return new PaymentCancellationInfo("payKey", purchase.getOrderId(), "CANCELED", "CARD", purchase.getAmount(),
        "https://receipt.example.com",
        List.of(new PaymentCancellationInfo.CancelDetail(purchase.getAmount(), "2026-03-26T08:01:00+09:00",
            "Local payment finalization failed after PG confirm")));
  }
}
