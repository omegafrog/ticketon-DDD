package org.codenbug.purchase.infra.es;

import java.util.List;

import org.codenbug.purchase.domain.es.PurchaseStoredEvent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaPurchaseEventStoreRepository extends JpaRepository<PurchaseStoredEvent, Long> {
	boolean existsByPurchaseIdAndCommandId(String purchaseId, String commandId);
	List<PurchaseStoredEvent> findByPurchaseIdOrderBySeqAsc(String purchaseId);
}
