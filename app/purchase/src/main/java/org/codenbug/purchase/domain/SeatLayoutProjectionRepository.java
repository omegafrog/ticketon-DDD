package org.codenbug.purchase.domain;

import org.codenbug.purchase.query.model.SeatLayoutProjection;

public interface SeatLayoutProjectionRepository {
	SeatLayoutProjection findById(Long seatLayoutId);
	
	boolean existsById(Long layoutId);
	
	SeatLayoutProjection save(SeatLayoutProjection seatLayoutProjection);
}
