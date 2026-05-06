package org.codenbug.purchase.domain.port;

import org.codenbug.purchase.domain.Purchase;
import org.codenbug.purchase.domain.Refund;
import org.codenbug.purchase.domain.RefundId;
import org.codenbug.purchase.domain.RefundStatus;
import org.codenbug.purchase.domain.UserId;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * 환불 도메인 Repository 인터페이스
 */
public interface RefundRepository {
    
    Refund save(Refund refund);
    
    Optional<Refund> findById(RefundId refundId);
    
    List<Refund> findByPurchaseId(String purchaseId);
    
    Page<Refund> findByPurchaseUserId(UserId userId, Pageable pageable);
    
    List<Refund> findByStatus(RefundStatus status);
    
    List<Refund> findByPurchaseAndStatus(Purchase purchase, RefundStatus status);
    
    boolean existsByPurchaseAndStatus(Purchase purchase, RefundStatus status);
    
    void delete(Refund refund);
    
    void deleteAll(List<Refund> refunds);
}
