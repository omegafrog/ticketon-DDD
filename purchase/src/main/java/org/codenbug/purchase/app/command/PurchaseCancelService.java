package org.codenbug.purchase.app.command;

import org.codenbug.purchase.app.PurchaseObservation;
import org.codenbug.purchase.domain.port.PGApiService;
import org.codenbug.purchase.domain.port.PurchaseCancelStore;
import org.codenbug.purchase.domain.port.PurchaseRepository;
import org.codenbug.purchase.domain.port.RefundNotificationPort;
import java.time.OffsetDateTime;

import org.codenbug.notification.domain.entity.NotificationType;
import org.codenbug.purchase.domain.port.MessagePublisher;
import org.codenbug.purchase.domain.PaymentCancellationInfo;
import org.codenbug.purchase.domain.Purchase;
import org.codenbug.purchase.domain.PurchaseCancel;
import org.codenbug.purchase.domain.Refund;
import org.codenbug.purchase.domain.RefundDomainService;
import org.codenbug.purchase.domain.port.RefundRepository;
import org.codenbug.purchase.domain.UserId;
import org.codenbug.purchase.domain.event.RefundCompletedEvent;
import org.codenbug.purchase.ui.request.CancelPaymentRequest;
import org.codenbug.purchase.ui.response.CancelPaymentResponse;
import org.codenbug.redislock.RedisLockService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class PurchaseCancelService {
  private final PGApiService pgApiService;
  private final PurchaseRepository purchaseRepository;
  private final PurchaseCancelStore purchaseCancelRepository;
  private final MessagePublisher publisher;
  private final RefundNotificationPort notificationEventPublisher;
  private final RefundDomainService refundDomainService;
  private final RefundRepository refundRepository;
  private final RedisLockService redisLockService;
  private final PurchaseObservation purchaseObservation;

  public PurchaseCancelService(PGApiService pgApiService, PurchaseRepository purchaseRepository,
      PurchaseCancelStore purchaseCancelRepository, MessagePublisher publisher,
      RefundNotificationPort notificationEventPublisher, RefundDomainService refundDomainService,
      RefundRepository refundRepository, RedisLockService redisLockService) {
    this(pgApiService, purchaseRepository, purchaseCancelRepository, publisher, notificationEventPublisher,
        refundDomainService, refundRepository, redisLockService, PurchaseObservation.noop());
  }

  @org.springframework.beans.factory.annotation.Autowired
  public PurchaseCancelService(PGApiService pgApiService, PurchaseRepository purchaseRepository,
      PurchaseCancelStore purchaseCancelRepository, MessagePublisher publisher,
      RefundNotificationPort notificationEventPublisher, RefundDomainService refundDomainService,
      RefundRepository refundRepository, RedisLockService redisLockService,
      PurchaseObservation purchaseObservation) {
    this.pgApiService = pgApiService;
    this.purchaseRepository = purchaseRepository;
    this.purchaseCancelRepository = purchaseCancelRepository;
    this.publisher = publisher;
    this.notificationEventPublisher = notificationEventPublisher;
    this.refundDomainService = refundDomainService;
    this.refundRepository = refundRepository;
    this.redisLockService = redisLockService;
    this.purchaseObservation = purchaseObservation;
  }

  @Transactional
  @org.codenbug.notification.aop.NotifyUser(type = NotificationType.PAYMENT, title = "환불 완료", content = "티켓 환불이 성공적으로 처리되었습니다.", userIdExpression = "#userId")
  public CancelPaymentResponse cancelPayment(CancelPaymentRequest request, String paymentKey, String userId) {
    try {
      Purchase purchase = purchaseRepository.findByPid(paymentKey)
          .orElseThrow(() -> new IllegalArgumentException("[cancel] 해당 결제 정보를 찾을 수 없습니다."));

      PaymentCancellationInfo canceledPaymentInfo = pgApiService.cancelPayment(paymentKey, request.getCancelReason());

      int refundAmount = canceledPaymentInfo.getTotalCancelAmount();

      RefundDomainService.RefundResult refundResult = refundDomainService.processUserRefund(
          purchase,
          refundAmount,
          request.getCancelReason(),
          new UserId(userId));

      Refund savedRefund = refundRepository.save(refundResult.getRefund());
      refundDomainService.completeRefundWithPaymentInfo(savedRefund, canceledPaymentInfo);
      refundRepository.save(savedRefund);
      purchaseObservation.recordRefundResult(savedRefund.getStatus());

      purchaseRepository.save(purchase);
      saveLegacyPurchaseCancel(purchase, canceledPaymentInfo);
      publishRefundCompletedEvent(purchase, refundAmount, request.getCancelReason(), canceledPaymentInfo);

      publisher.publishSeatPurchaseCanceledEvent(refundResult.getSeatIds(), purchase.getPurchaseId().getValue());

      return CancelPaymentResponse.of(canceledPaymentInfo);
    } catch (Exception e) {
      log.error("[cancelPayment] 결제 취소 처리 중 예외 발생 - userId: {}, paymentKey: {}, 오류: {}",
          userId, paymentKey, e.getMessage(), e);
      redisLockService.releaseAllLocks(userId);
      redisLockService.releaseAllEntryQueueLocks(userId);
      throw e;
    }
  }

  private void saveLegacyPurchaseCancel(Purchase purchase, PaymentCancellationInfo canceledPaymentInfo) {
    for (PaymentCancellationInfo.CancelDetail cancelDetail : canceledPaymentInfo.getCancels()) {
      PurchaseCancel purchaseCancel = PurchaseCancel.builder()
          .purchase(purchase)
          .cancelAmount(cancelDetail.getCancelAmount())
          .cancelReason(cancelDetail.getCancelReason())
          .canceledAt(OffsetDateTime.parse(cancelDetail.getCanceledAt()).toLocalDateTime())
          .receiptUrl(canceledPaymentInfo.getReceiptUrl())
          .build();

      purchaseCancelRepository.save(purchaseCancel);
    }
  }

  private void publishRefundCompletedEvent(Purchase purchase, int refundAmount, String cancelReason,
      PaymentCancellationInfo canceledPaymentInfo) {
    try {
      RefundCompletedEvent refundEvent = RefundCompletedEvent.of(
          purchase.getUserId().getValue(),
          purchase.getPurchaseId().getValue(),
          purchase.getOrderId(),
          purchase.getOrderName(),
          refundAmount,
          cancelReason,
          canceledPaymentInfo.getFirstCanceledAt(),
          purchase.getOrderName());

      notificationEventPublisher.publishRefundCompletedEvent(refundEvent);
    } catch (Exception e) {
      log.error("환불 완료 알림 이벤트 발행 실패. 사용자ID: {}, 구매ID: {}, 오류: {}",
          purchase.getUserId().getValue(), purchase.getPurchaseId().getValue(), e.getMessage(), e);
    }
  }
}
