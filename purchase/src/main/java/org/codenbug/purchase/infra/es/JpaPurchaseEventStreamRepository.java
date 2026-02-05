package org.codenbug.purchase.infra.es;

import org.codenbug.purchase.domain.es.PurchaseEventStream;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaPurchaseEventStreamRepository extends JpaRepository<PurchaseEventStream, String> {
}
