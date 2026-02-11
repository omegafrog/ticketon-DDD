package org.codenbug.seat.global;

import java.util.List;
import java.util.Set;

import org.codenbug.seat.domain.Seat;

import lombok.Getter;

@Getter
public class UpdateSeatLayoutRequest {
	private String layout;
	private List<Seat> seats;

	protected UpdateSeatLayoutRequest() {
	}

	public UpdateSeatLayoutRequest(String layout, List<Seat> seats) {
		this.layout = layout;
		this.seats = seats;
	}
}
