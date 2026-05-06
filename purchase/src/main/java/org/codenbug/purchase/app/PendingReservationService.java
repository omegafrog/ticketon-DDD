package org.codenbug.purchase.app;

import java.time.LocalDateTime;
import java.util.List;

import org.codenbug.common.exception.AccessDeniedException;
import org.codenbug.purchase.domain.PaymentStatus;
import org.codenbug.purchase.domain.Purchase;
import org.codenbug.purchase.domain.PurchaseId;
import org.codenbug.redislock.RedisLockService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PendingReservationService {

  private final PurchaseRepository purchaseRepository;
  private final RedisLockService redisLockService;
  private final PurchaseObservation purchaseObservation;

  public PendingReservationService(PurchaseRepository purchaseRepository, RedisLockService redisLockService) {
    this(purchaseRepository, redisLockService, PurchaseObservation.noop());
  }

  @org.springframework.beans.factory.annotation.Autowired
  public PendingReservationService(PurchaseRepository purchaseRepository, RedisLockService redisLockService,
      PurchaseObservation purchaseObservation) {
    this.purchaseRepository = purchaseRepository;
    this.redisLockService = redisLockService;
    this.purchaseObservation = purchaseObservation;
  }

  @Transactional
  public void cancelPendingReservation(String purchaseId, String userId) {
    Purchase purchase = purchaseRepository.findById(new PurchaseId(purchaseId))
        .orElseThrow(() -> new IllegalArgumentException("구매 정보를 찾을 수 없습니다."));
    if (!purchase.getUserId().getValue().equals(userId)) {
      throw new AccessDeniedException("해당 구매 정보에 대한 접근 권한이 없습니다.");
    }

    purchase.cancelPending();
    purchaseRepository.save(purchase);
    releaseSeatAndEntryToken(userId);
  }

  @Transactional
  public int expireOverdueReservations(LocalDateTime now) {
    List<Purchase> overduePurchases = purchaseRepository
        .findByPaymentStatusAndPaymentDeadlineAtBefore(PaymentStatus.IN_PROGRESS, now);
    for (Purchase purchase : overduePurchases) {
      purchase.expireIfOverdue(now);
      purchaseRepository.save(purchase);
      releaseSeatAndEntryToken(purchase.getUserId().getValue());
    }
    purchaseObservation.recordReservationExpired(overduePurchases.size());
    return overduePurchases.size();
  }

  private void releaseSeatAndEntryToken(String userId) {
    redisLockService.releaseAllLocks(userId);
    redisLockService.releaseAllEntryQueueLocks(userId);
  }
}
