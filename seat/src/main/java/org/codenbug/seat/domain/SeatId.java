package org.codenbug.seat.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public class SeatId {
	@Column(name = "seat_id")
	private String value;
}
