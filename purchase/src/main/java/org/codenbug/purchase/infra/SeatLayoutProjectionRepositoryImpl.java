package org.codenbug.purchase.infra;

import org.codenbug.purchase.domain.SeatLayoutProjectionRepository;
import org.codenbug.purchase.query.model.SeatLayoutProjection;
import org.springframework.stereotype.Repository;

import jakarta.persistence.EntityNotFoundException;

@Repository
public class SeatLayoutProjectionRepositoryImpl implements SeatLayoutProjectionRepository {
	private final JpaSeatLayoutProjectionRepository jpaRepository;

	public SeatLayoutProjectionRepositoryImpl(JpaSeatLayoutProjectionRepository jpaRepository) {
		this.jpaRepository = jpaRepository;
	}

	@Override
	public SeatLayoutProjection findById(Long seatLayoutId) {
		return jpaRepository.findById(seatLayoutId)
			.orElseThrow(() -> new EntityNotFoundException());
	}
}
