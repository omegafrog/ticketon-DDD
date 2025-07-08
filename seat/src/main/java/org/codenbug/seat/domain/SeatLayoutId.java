package org.codenbug.seat.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.Getter;

@Embeddable
@Getter
public class SeatLayoutId {
	@Column(name = "seat_layout_id", nullable = true)
	private Long value;

	protected SeatLayoutId() {}
	public SeatLayoutId(Long value) {
		this.value = value;
	}
}
