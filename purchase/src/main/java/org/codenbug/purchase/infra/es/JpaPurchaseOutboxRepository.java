package org.codenbug.purchase.infra.es;

import java.util.List;

import org.codenbug.purchase.domain.es.PurchaseOutboxMessage;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface JpaPurchaseOutboxRepository extends JpaRepository<PurchaseOutboxMessage, Long> {
	@Query("""
		select m from PurchaseOutboxMessage m
		where m.publishedAt is null
		order by m.id asc
	""")
	List<PurchaseOutboxMessage> findUnpublished(Pageable pageable);
}
