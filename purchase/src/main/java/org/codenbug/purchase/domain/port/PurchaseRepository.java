package org.codenbug.purchase.domain.port;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.codenbug.purchase.domain.PaymentStatus;
import org.codenbug.purchase.domain.Purchase;
import org.codenbug.purchase.domain.PurchaseId;
import org.codenbug.purchase.domain.UserId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface PurchaseRepository {
  Optional<Purchase> findById(PurchaseId purchaseId);

  Optional<Purchase> findByPid(String pid);

  Purchase save(Purchase purchase);

  boolean existsByUserIdAndPaymentStatus(UserId userId, PaymentStatus paymentStatus);

  long countByPaymentStatus(PaymentStatus paymentStatus);

  List<Purchase> findByPaymentStatusAndPaymentDeadlineAtBefore(PaymentStatus paymentStatus, LocalDateTime deadline);

  List<Purchase> findByEventIdAndPaymentStatus(String eventId, PaymentStatus paymentStatus);

  Page<Purchase> findByUserIdAndPaymentStatusInOrderByCreatedAtDesc(UserId userId, List<PaymentStatus> statuses,
      Pageable pageable);

  boolean existsByOrderId(String orderId);
}
