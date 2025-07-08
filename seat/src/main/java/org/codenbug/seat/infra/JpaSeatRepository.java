package org.codenbug.seat.infra;

import org.codenbug.seat.domain.SeatLayout;
import org.codenbug.seat.domain.SeatLayoutId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaSeatRepository extends JpaRepository<SeatLayout, SeatLayoutId> {

	SeatLayout findSeatLayoutById(SeatLayoutId id);
}
