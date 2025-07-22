package org.codenbug.seat.global;

import java.util.List;

import lombok.Getter;

@Getter
public class SeatLayoutResponse {
	private Long id;
	private String seatLayout;
	private List<SeatDto> seats;
	private String hallName;
	private String locationName;

	protected SeatLayoutResponse() {
	}

	public SeatLayoutResponse(Long id, String seatLayout, List<SeatDto> seats, String hallName, String locationName) {
		this.id = id;
		this.seatLayout = seatLayout;
		this.seats = seats;
		this.hallName = hallName;
		this.locationName = locationName;
	}

}
