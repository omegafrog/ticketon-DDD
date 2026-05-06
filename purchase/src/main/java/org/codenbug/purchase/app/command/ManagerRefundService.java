package org.codenbug.purchase.app.command;

import org.codenbug.purchase.app.PurchaseObservation;
import org.codenbug.purchase.domain.port.PGApiService;
import org.codenbug.purchase.domain.port.PurchaseRepository;
import org.codenbug.purchase.domain.port.RefundNotificationPort;
import java.util.List;

import org.codenbug.purchase.domain.port.MessagePublisher;
import org.codenbug.purchase.domain.PaymentCancellationInfo;
import org.codenbug.purchase.domain.PaymentStatus;
import org.codenbug.purchase.domain.Purchase;
import org.codenbug.purchase.domain.PurchaseId;
import org.codenbug.purchase.domain.Refund;
import org.codenbug.purchase.domain.RefundDomainService;
import org.codenbug.purchase.domain.port.RefundRepository;
import org.codenbug.purchase.domain.port.EventInfoProvider;
import org.codenbug.purchase.domain.EventSummary;
import org.codenbug.purchase.domain.UserId;
import org.codenbug.purchase.domain.event.ManagerRefundCompletedEvent;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 매니저 환불 전용 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor(onConstructor_ = @org.springframework.beans.factory.annotation.Autowired)
@Transactional
public class ManagerRefundService {
  private static final int MAX_TOSS_CANCEL_RETRIES = 5;

  private final PGApiService pgApiService;
  private final PurchaseRepository purchaseRepository;
  private final RefundRepository refundRepository;
  private final RefundDomainService refundDomainService;
  private final RefundNotificationPort notificationEventPublisher;
  private final MessagePublisher messagePublisher;
  private final EventInfoProvider eventInfoProvider;
  private final PurchaseObservation purchaseObservation;

  public ManagerRefundService(PGApiService pgApiService, PurchaseRepository purchaseRepository,
      RefundRepository refundRepository, RefundDomainService refundDomainService,
      RefundNotificationPort notificationEventPublisher, MessagePublisher messagePublisher,
      EventInfoProvider eventInfoProvider) {
    this.pgApiService = pgApiService;
    this.purchaseRepository = purchaseRepository;
    this.refundRepository = refundRepository;
    this.refundDomainService = refundDomainService;
    this.notificationEventPublisher = notificationEventPublisher;
    this.messagePublisher = messagePublisher;
    this.eventInfoProvider = eventInfoProvider;
    this.purchaseObservation = PurchaseObservation.noop();
  }

  /**
   * 매니저에 의한 단일 구매 환불
   */
  public ManagerRefundResult processManagerRefund(String purchaseId, String refundReason,
      String managerName, UserId managerId) {
    log.info("매니저 환불 처리 시작: purchaseId={}, managerId={}", purchaseId, managerId.getValue());

    // 1. 구매 정보 조회
    Purchase purchase = purchaseRepository.findById(new PurchaseId(purchaseId))
        .orElseThrow(() -> new IllegalArgumentException("해당 구매 정보를 찾을 수 없습니다."));

    // 2. 환불 가능 여부 확인
    if (!purchase.canRefund()) {
      throw new IllegalStateException("환불할 수 없는 구매입니다: " + purchase.getPaymentStatus());
    }
    validateManagerOwnsEvent(purchase.getEventId(), managerId);

    // 3. 외부 결제 시스템 환불 요청 (전액 환불)
    String paymentKey = purchase.getPaymentKey();
    PaymentCancellationInfo canceledPaymentInfo = cancelPaymentWithRetry(purchase, paymentKey,
        "매니저 환불: " + refundReason, refundReason, managerId);

    // 4. 환불 금액 계산
    int refundAmount = canceledPaymentInfo.getTotalCancelAmount();

    // 5. 도메인 서비스를 통한 매니저 환불 처리
    RefundDomainService.RefundResult refundResult = refundDomainService.processManagerRefund(
        purchase, refundAmount, refundReason, managerId);

    // 6. 환불 엔티티 저장 및 완료 처리
    Refund savedRefund = refundRepository.save(refundResult.getRefund());
    refundDomainService.completeRefundWithPaymentInfo(savedRefund, canceledPaymentInfo);
    refundRepository.save(savedRefund);
    purchaseObservation.recordRefundResult(savedRefund.getStatus());

    // 7. 구매 정보 저장
    purchaseRepository.save(purchase);

    // 8. 매니저 환불 알림 이벤트 발행
    publishManagerRefundEvent(purchase, refundAmount, refundReason, managerName, canceledPaymentInfo);

    // 9. 좌석 취소 이벤트 발행
    messagePublisher.publishSeatPurchaseCanceledEvent(refundResult.getSeatIds(), purchaseId);

    log.info("매니저 환불 처리 완료: purchaseId={}, refundId={}",
        purchaseId, savedRefund.getRefundId().getValue());

    return ManagerRefundResult.builder()
        .refund(savedRefund)
        .purchase(purchase)
        .refundAmount(refundAmount)
        .seatIds(refundResult.getSeatIds())
        .build();
  }

  /**
   * 특정 이벤트의 모든 구매에 대한 일괄 환불 (공연 취소 등)
   */
  public List<ManagerRefundResult> processBatchRefund(String eventId, String refundReason,
      String managerName, UserId managerId) {
    log.info("일괄 환불 처리 시작: eventId={}, managerId={}", eventId, managerId.getValue());
    validateManagerOwnsEvent(eventId, managerId);

    // 해당 이벤트의 모든 완료된 구매 조회
    List<Purchase> purchases = purchaseRepository.findByEventIdAndPaymentStatus(eventId, PaymentStatus.DONE);

    return purchases.stream()
        .map(purchase -> {
          try {
            return processManagerRefund(
                purchase.getPurchaseId().getValue(),
                refundReason,
                managerName,
                managerId);
          } catch (Exception e) {
            log.error("일괄 환불 중 개별 환불 실패: purchaseId={}, error={}",
                purchase.getPurchaseId().getValue(), e.getMessage());
            // 개별 실패는 전체 처리를 중단하지 않음
            return null;
          }
        })
        .filter(java.util.Objects::nonNull)
        .toList();
  }

  /**
   * 매니저 환불 알림 이벤트 발행
   */
  private void publishManagerRefundEvent(Purchase purchase, int refundAmount, String refundReason,
      String managerName, PaymentCancellationInfo canceledPaymentInfo) {
    try {
      ManagerRefundCompletedEvent event = ManagerRefundCompletedEvent.of(
          purchase.getUserId().getValue(),
          purchase.getPurchaseId().getValue(),
          purchase.getOrderId(),
          purchase.getOrderName(),
          refundAmount,
          refundReason,
          canceledPaymentInfo.getFirstCanceledAt(),
          purchase.getOrderName(),
          managerName);

      notificationEventPublisher.publishManagerRefundCompletedEvent(event);
    } catch (Exception e) {
      log.error("매니저 환불 알림 이벤트 발행 실패: purchaseId={}, managerName={}",
          purchase.getPurchaseId().getValue(), managerName, e);
    }
  }

  private void validateManagerOwnsEvent(String eventId, UserId managerId) {
    EventSummary eventSummary = eventInfoProvider.getEventSummary(eventId);
    if (eventSummary.getManagerId() == null || !eventSummary.getManagerId().equals(managerId.getValue())) {
      throw new org.codenbug.common.exception.AccessDeniedException("Manager can refund only own events.");
    }
  }

  private PaymentCancellationInfo cancelPaymentWithRetry(Purchase purchase, String paymentKey,
      String providerReason, String refundReason, UserId managerId) {
    RuntimeException lastFailure = null;
    Refund failedRefund = Refund.createManagerRefund(purchase, purchase.getTotalAmount(), refundReason, managerId);
    failedRefund.startProcessing();

    for (int attempt = 1; attempt <= MAX_TOSS_CANCEL_RETRIES; attempt++) {
      try {
        return pgApiService.cancelPayment(paymentKey, providerReason,
            "refund:" + purchase.getPurchaseId().getValue() + ":" + attempt);
      } catch (RuntimeException e) {
        lastFailure = e;
        failedRefund.recordRetryFailure(e.getMessage(), MAX_TOSS_CANCEL_RETRIES);
      }
    }

    refundRepository.save(failedRefund);
    purchaseObservation.recordRefundResult(failedRefund.getStatus());
    throw lastFailure == null ? new IllegalStateException("Toss cancel failed") : lastFailure;
  }

  /**
   * 매니저 환불 결과 DTO
   */
  @lombok.Builder
  @lombok.Getter
  public static class ManagerRefundResult {
    private final Refund refund;
    private final Purchase purchase;
    private final int refundAmount;
    private final List<String> seatIds;

    public String getRefundId() {
      return refund.getRefundId().getValue();
    }

    public String getPurchaseId() {
      return purchase.getPurchaseId().getValue();
    }

    public String getUserId() {
      return purchase.getUserId().getValue();
    }
  }
}
