package org.codenbug.purchase.infra;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.codenbug.purchase.domain.port.PurchaseRepository;
import org.codenbug.purchase.domain.PaymentStatus;
import org.codenbug.purchase.domain.Purchase;
import org.codenbug.purchase.domain.PurchaseId;
import org.codenbug.purchase.domain.UserId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

@Component
class PurchaseStoreAdapter implements PurchaseRepository {
  private final org.codenbug.purchase.infra.PurchaseRepository purchaseRepository;

  PurchaseStoreAdapter(org.codenbug.purchase.infra.PurchaseRepository purchaseRepository) {
    this.purchaseRepository = purchaseRepository;
  }

  @Override
  public Optional<Purchase> findById(PurchaseId purchaseId) {
    return purchaseRepository.findById(purchaseId);
  }

  @Override
  public Optional<Purchase> findByPid(String pid) {
    return purchaseRepository.findByPid(pid);
  }

  @Override
  public Purchase save(Purchase purchase) {
    return purchaseRepository.save(purchase);
  }

  @Override
  public boolean existsByUserIdAndPaymentStatus(UserId userId, PaymentStatus paymentStatus) {
    return purchaseRepository.existsByUserIdAndPaymentStatus(userId, paymentStatus);
  }

  @Override
  public long countByPaymentStatus(PaymentStatus paymentStatus) {
    return purchaseRepository.countByPaymentStatus(paymentStatus);
  }

  @Override
  public List<Purchase> findByPaymentStatusAndPaymentDeadlineAtBefore(PaymentStatus paymentStatus,
      LocalDateTime deadline) {
    return purchaseRepository.findByPaymentStatusAndPaymentDeadlineAtBefore(paymentStatus, deadline);
  }

  @Override
  public List<Purchase> findByEventIdAndPaymentStatus(String eventId, PaymentStatus paymentStatus) {
    return purchaseRepository.findByEventIdAndPaymentStatus(eventId, paymentStatus);
  }

  @Override
  public Page<Purchase> findByUserIdAndPaymentStatusInOrderByCreatedAtDesc(UserId userId,
      List<PaymentStatus> statuses, Pageable pageable) {
    return purchaseRepository.findByUserIdAndPaymentStatusInOrderByCreatedAtDesc(userId, statuses, pageable);
  }

  @Override
  public boolean existsByOrderId(String orderId) {
    return purchaseRepository.existsByOrderId(orderId);
  }
}
