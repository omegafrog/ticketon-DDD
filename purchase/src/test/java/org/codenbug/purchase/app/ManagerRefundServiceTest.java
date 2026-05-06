package org.codenbug.purchase.app;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.codenbug.common.exception.AccessDeniedException;
import org.codenbug.purchase.domain.EventInfoProvider;
import org.codenbug.purchase.domain.EventSummary;
import org.codenbug.purchase.domain.MessagePublisher;
import org.codenbug.purchase.domain.PaymentMethod;
import org.codenbug.purchase.domain.Purchase;
import org.codenbug.purchase.domain.PurchaseId;
import org.codenbug.purchase.domain.Refund;
import org.codenbug.purchase.domain.RefundDomainService;
import org.codenbug.purchase.domain.RefundRepository;
import org.codenbug.purchase.domain.RefundStatus;
import org.codenbug.purchase.domain.UserId;
import org.codenbug.purchase.infra.CanceledPaymentInfo;
import org.codenbug.purchase.infra.NotificationEventPublisher;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ManagerRefundServiceTest {

  @Mock
  private PGApiService pgApiService;

  @Mock
  private PurchaseRepository purchaseRepository;

  @Mock
  private RefundRepository refundRepository;

  @Mock
  private NotificationEventPublisher notificationEventPublisher;

  @Mock
  private MessagePublisher messagePublisher;

  @Mock
  private EventInfoProvider eventInfoProvider;

  @Test
  @DisplayName("작성 매니저는 단건 전체 환불을 처리할 수 있다")
  void 작성_매니저_단건_전체_환불_처리_성공() {
    Purchase purchase = completedPurchase("event-1", "pay-key");
    when(purchaseRepository.findById(any(PurchaseId.class))).thenReturn(Optional.of(purchase));
    when(eventInfoProvider.getEventSummary("event-1")).thenReturn(eventSummary("event-1", "manager-1"));
    when(pgApiService.cancelPayment(eq("pay-key"), anyString(), anyString())).thenReturn(canceledPaymentInfo(1000));
    when(refundRepository.save(any(Refund.class))).thenAnswer(invocation -> invocation.getArgument(0));
    ManagerRefundService service = service();

    ManagerRefundService.ManagerRefundResult result = service.processManagerRefund(
        purchase.getPurchaseId().getValue(), "weather", "manager", new UserId("manager-1"));

    assertThat(result.getRefund().getStatus()).isEqualTo(RefundStatus.COMPLETED);
    assertThat(result.getRefundAmount()).isEqualTo(1000);
    assertThat(purchase.getPaymentStatus().name()).isEqualTo("REFUNDED");
    verify(messagePublisher).publishSeatPurchaseCanceledEvent(List.of(), purchase.getPurchaseId().getValue());
  }

  @Test
  @DisplayName("이벤트 작성자가 아닌 매니저는 환불할 수 없다")
  void 이벤트_작성자가_아닌_매니저_환불_거부() {
    Purchase purchase = completedPurchase("event-1", "pay-key");
    when(purchaseRepository.findById(any(PurchaseId.class))).thenReturn(Optional.of(purchase));
    when(eventInfoProvider.getEventSummary("event-1")).thenReturn(eventSummary("event-1", "manager-1"));
    ManagerRefundService service = service();

    assertThatThrownBy(() -> service.processManagerRefund(
        purchase.getPurchaseId().getValue(), "weather", "manager", new UserId("manager-2")))
        .isInstanceOf(AccessDeniedException.class);

    verify(pgApiService, never()).cancelPayment(anyString(), anyString(), anyString());
    verify(refundRepository, never()).save(any());
  }

  @Test
  @DisplayName("일괄 환불은 작성 매니저의 이벤트 구매 전체를 대상으로 요청한다")
  void 일괄_환불시_작성_매니저_이벤트_구매_대상() {
    Purchase purchase1 = completedPurchase("event-1", "pay-key-1");
    Purchase purchase2 = completedPurchase("event-1", "pay-key-2");
    when(eventInfoProvider.getEventSummary("event-1")).thenReturn(eventSummary("event-1", "manager-1"));
    when(purchaseRepository.findByEventIdAndPaymentStatus(eq("event-1"), any()))
        .thenReturn(List.of(purchase1, purchase2));
    when(purchaseRepository.findById(any(PurchaseId.class))).thenReturn(Optional.of(purchase1), Optional.of(purchase2));
    when(pgApiService.cancelPayment(anyString(), anyString(), anyString())).thenReturn(canceledPaymentInfo(1000));
    when(refundRepository.save(any(Refund.class))).thenAnswer(invocation -> invocation.getArgument(0));
    ManagerRefundService service = service();

    List<ManagerRefundService.ManagerRefundResult> results = service.processBatchRefund("event-1", "weather", "manager",
        new UserId("manager-1"));

    assertThat(results).hasSize(2);
    verify(pgApiService, times(2)).cancelPayment(anyString(), anyString(), anyString());
  }

  @Test
  @DisplayName("Toss cancel이 5회 실패하면 Refund를 실패 상태로 저장한다")
  void Toss_취소_5회_실패시_실패_상태_환불_저장() {
    Purchase purchase = completedPurchase("event-1", "pay-key");
    when(purchaseRepository.findById(any(PurchaseId.class))).thenReturn(Optional.of(purchase));
    when(eventInfoProvider.getEventSummary("event-1")).thenReturn(eventSummary("event-1", "manager-1"));
    when(pgApiService.cancelPayment(eq("pay-key"), anyString(), anyString()))
        .thenThrow(new RuntimeException("toss failed"));
    ManagerRefundService service = service();

    assertThatThrownBy(() -> service.processManagerRefund(
        purchase.getPurchaseId().getValue(), "weather", "manager", new UserId("manager-1")))
        .isInstanceOf(RuntimeException.class)
        .hasMessageContaining("toss failed");

    ArgumentCaptor<Refund> refundCaptor = ArgumentCaptor.forClass(Refund.class);
    verify(pgApiService, times(5)).cancelPayment(eq("pay-key"), anyString(), anyString());
    verify(refundRepository).save(refundCaptor.capture());
    assertThat(refundCaptor.getValue().getStatus()).isEqualTo(RefundStatus.FAILED);
    assertThat(refundCaptor.getValue().getRetryCount()).isEqualTo(5);
  }

  private ManagerRefundService service() {
    return new ManagerRefundService(pgApiService, purchaseRepository, refundRepository,
        new RefundDomainService(), notificationEventPublisher, messagePublisher, eventInfoProvider);
  }

  private Purchase completedPurchase(String eventId, String paymentKey) {
    Purchase purchase = new Purchase(eventId, "order-1", 1000, 1L, new UserId("user-1"));
    purchase.updatePaymentInfo(paymentKey, eventId, 1000, PaymentMethod.카드, "order-name", LocalDateTime.now());
    purchase.markAsCompleted();
    return purchase;
  }

  private EventSummary eventSummary(String eventId, String managerId) {
    return new EventSummary(eventId, 1L, true, "OPEN", 1L, 1L, "event", managerId);
  }

  private org.codenbug.purchase.domain.PaymentCancellationInfo canceledPaymentInfo(int amount) {
    return new CanceledPaymentInfo("pay-key", "order-1", "CANCELED", "CARD", amount,
        new CanceledPaymentInfo.Receipt("https://receipt.test"),
        List.of(new CanceledPaymentInfo.CancelDetail(amount, "2026-04-28T00:00:00+09:00", "weather"))).toDomain();
  }
}
