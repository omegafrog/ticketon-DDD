package org.codenbug.seat.infra;

import org.codenbug.seat.domain.SeatLayout;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaSeatRepository extends JpaRepository<SeatLayout, Long> {

	SeatLayout findSeatLayoutById(Long id);
}
