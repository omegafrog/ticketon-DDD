package org.codenbug.purchase.infra;

import java.util.List;

import org.codenbug.purchase.domain.Purchase;
import org.codenbug.purchase.domain.PurchaseId;
import org.codenbug.purchase.domain.Refund;
import org.codenbug.purchase.domain.RefundId;
import org.codenbug.purchase.domain.RefundStatus;
import org.codenbug.purchase.domain.UserId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * JPA 기반 환불 Repository
 */
public interface JpaRefundRepository extends JpaRepository<Refund, RefundId> {
    
    List<Refund> findByPurchase(Purchase purchase);
    List<Refund> findByPurchase_PurchaseId(PurchaseId purchasePurchaseId);
    @Query("SELECT r FROM Refund r WHERE r.purchase.userId = :userId")
    Page<Refund> findByPurchaseUserId(@Param("userId") UserId userId, Pageable pageable);
    
    List<Refund> findByStatus(RefundStatus status);
    
    List<Refund> findByPurchaseAndStatus(Purchase purchase, RefundStatus status);
    
    boolean existsByPurchaseAndStatus(Purchase purchase, RefundStatus status);
    
    @Query("SELECT r FROM Refund r WHERE r.purchase.userId = :userId AND r.status = :status")
    List<Refund> findByUserIdAndStatus(@Param("userId") UserId userId, @Param("status") RefundStatus status);
    
    @Query("SELECT COUNT(r) FROM Refund r WHERE r.purchase.userId = :userId AND r.status = :status")
    long countByUserIdAndStatus(@Param("userId") UserId userId, @Param("status") RefundStatus status);
}