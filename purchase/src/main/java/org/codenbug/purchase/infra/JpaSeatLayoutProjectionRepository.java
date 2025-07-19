package org.codenbug.purchase.infra;

import org.codenbug.purchase.query.model.SeatLayoutProjection;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaSeatLayoutProjectionRepository extends JpaRepository<SeatLayoutProjection, Long> {

}
