package org.codenbug.purchase.infra.es;

import org.codenbug.purchase.domain.es.PurchaseConfirmStatusProjection;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaPurchaseConfirmStatusProjectionRepository extends JpaRepository<PurchaseConfirmStatusProjection, String> {
}
