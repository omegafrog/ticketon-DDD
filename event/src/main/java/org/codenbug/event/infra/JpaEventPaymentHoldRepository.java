package org.codenbug.event.infra;

import java.time.LocalDateTime;
import java.util.Optional;

import org.codenbug.event.domain.EventPaymentHold;
import org.codenbug.event.domain.EventPaymentHoldStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface JpaEventPaymentHoldRepository extends JpaRepository<EventPaymentHold, String> {
	@Query("""
		select count(h) > 0
		from EventPaymentHold h
		where h.eventId = :eventId
		  and h.status = :status
		  and h.expiresAt > :now
	""")
	boolean existsActiveHold(@Param("eventId") String eventId, @Param("status") EventPaymentHoldStatus status,
		@Param("now") LocalDateTime now);

	@Query("""
		select h
		from EventPaymentHold h
		where h.eventId = :eventId
		  and h.purchaseId = :purchaseId
		  and h.status = :status
		  and h.expiresAt > :now
	""")
	Optional<EventPaymentHold> findActiveHoldForPurchase(@Param("eventId") String eventId,
		@Param("purchaseId") String purchaseId, @Param("status") EventPaymentHoldStatus status,
		@Param("now") LocalDateTime now);
}
