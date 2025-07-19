package org.codenbug.seat.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.EqualsAndHashCode;
import lombok.Getter;

@Embeddable
@EqualsAndHashCode(of="value")
@Getter
public class SeatId {
	@Column(name = "seat_id")
	private String value;
	protected SeatId(){}
	public SeatId(String value) {
		this.value = value;
	}
}
