package org.codenbug.purchase.app;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.codenbug.common.exception.AccessDeniedException;
import org.codenbug.purchase.domain.PaymentStatus;
import org.codenbug.purchase.domain.Purchase;
import org.codenbug.purchase.domain.PurchaseId;
import org.codenbug.purchase.domain.UserId;
import org.codenbug.redislock.RedisLockService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class PendingReservationServiceTest {

  @Mock
  private PurchaseRepository purchaseRepository;

  @Mock
  private RedisLockService redisLockService;

  @Test
  @DisplayName("결제 대기 예매 포기 시 예매를 취소하고 좌석과 입장 토큰을 해제한다")
  void 결제_대기_예매_포기시_예매_취소_리소스_해제() {
    Purchase purchase = purchase("user-1");
    when(purchaseRepository.findById(org.mockito.ArgumentMatchers.any(PurchaseId.class)))
        .thenReturn(Optional.of(purchase));
    PendingReservationService service = new PendingReservationService(purchaseRepository, redisLockService);

    service.cancelPendingReservation(purchase.getPurchaseId().getValue(), "user-1");

    assertThat(purchase.getPaymentStatus()).isEqualTo(PaymentStatus.CANCELED);
    verify(purchaseRepository).save(purchase);
    verify(redisLockService).releaseAllLocks("user-1");
    verify(redisLockService).releaseAllEntryQueueLocks("user-1");
  }

  @Test
  @DisplayName("타인의 결제 대기 예매는 포기할 수 없다")
  void 결제_대기_예매_포기시_타인_예매_거부() {
    Purchase purchase = purchase("user-1");
    when(purchaseRepository.findById(org.mockito.ArgumentMatchers.any(PurchaseId.class)))
        .thenReturn(Optional.of(purchase));
    PendingReservationService service = new PendingReservationService(purchaseRepository, redisLockService);

    assertThatThrownBy(() -> service.cancelPendingReservation(purchase.getPurchaseId().getValue(), "user-2"))
        .isInstanceOf(AccessDeniedException.class);
  }

  @Test
  @DisplayName("결제 제한시간이 지난 예매는 만료되고 좌석과 입장 토큰이 해제된다")
  void 결제_제한시간_지나면_예매_만료_리소스_해제() {
    Purchase purchase = purchase("user-1");
    LocalDateTime now = LocalDateTime.now();
    ReflectionTestUtils.setField(purchase, "paymentDeadlineAt", now.minusMinutes(1));
    when(purchaseRepository.findByPaymentStatusAndPaymentDeadlineAtBefore(PaymentStatus.IN_PROGRESS, now))
        .thenReturn(List.of(purchase));
    PendingReservationService service = new PendingReservationService(purchaseRepository, redisLockService);

    int expiredCount = service.expireOverdueReservations(now);

    assertThat(expiredCount).isEqualTo(1);
    assertThat(purchase.getPaymentStatus()).isEqualTo(PaymentStatus.EXPIRED);
    verify(purchaseRepository).save(purchase);
    verify(redisLockService).releaseAllLocks("user-1");
    verify(redisLockService).releaseAllEntryQueueLocks("user-1");
  }

  private Purchase purchase(String userId) {
    return new Purchase("event-1", "order-1", 1000, 1L, new UserId(userId));
  }
}
