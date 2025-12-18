package org.codenbug.purchase.infra;

import org.codenbug.purchase.query.model.EventProjection;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaPurchaseEventProjectionRepository extends JpaRepository<EventProjection, String> {
}
