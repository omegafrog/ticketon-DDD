package org.codenbug.purchase.infra;

import java.util.List;
import java.util.Optional;

import org.codenbug.purchase.domain.PaymentStatus;
import org.codenbug.purchase.domain.Purchase;
import org.codenbug.purchase.domain.UserId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PurchaseRepository extends JpaRepository<Purchase, String> {
	List<Purchase> findByUserIdAndPaymentStatusInOrderByCreatedAtDesc(String userId,
		List<PaymentStatus> statuses);

	Page<Purchase> findByUserIdAndPaymentStatusInOrderByCreatedAtDesc(UserId userId, List<PaymentStatus> statuses, Pageable pageable);

	Optional<Purchase> findByPid(String pid);

	@Query("""
    SELECT DISTINCT p FROM Purchase p
    JOIN p.tickets t
    WHERE t.eventId = :eventId
""")
	List<Purchase> findAllByEventId(@Param("eventId") Long eventId);

}
