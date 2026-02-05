package org.codenbug.purchase.infra.es;

import org.codenbug.purchase.domain.es.PurchaseProcessedMessage;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaPurchaseProcessedMessageRepository extends JpaRepository<PurchaseProcessedMessage, String> {
}
