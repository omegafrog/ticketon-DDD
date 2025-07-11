package org.codenbug.seat.global;

import java.util.List;

import lombok.Getter;

@Getter
public class SeatLayoutResponse {

	private String seatLayout;
	private List<SeatDto> seats;
	private String hallName;
	private String locationName;

	protected SeatLayoutResponse() {
	}

	public SeatLayoutResponse(String seatLayout, List<SeatDto> seats, String hallName, String locationName) {
		this.seatLayout = seatLayout;
		this.seats = seats;
		this.hallName = hallName;
		this.locationName = locationName;
	}

}
