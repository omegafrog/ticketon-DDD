package org.codenbug.seat.global;

import java.util.List;

import org.codenbug.seat.domain.RegionLocation;

import lombok.Getter;

@Getter
public class SeatLayoutResponse {
	private Long id;
	private String seatLayout;
	private List<SeatDto> seats;
	private String hallName;
	private String locationName;
	private RegionLocation regionLocation;

	protected SeatLayoutResponse() {
	}

	public SeatLayoutResponse(Long id, String seatLayout, List<SeatDto> seats, String hallName, String locationName) {
		this.id = id;
		this.seatLayout = seatLayout;
		this.seats = seats;
		this.hallName = hallName;
		this.locationName = locationName;
	}

	public SeatLayoutResponse(Long id, String seatLayout, List<SeatDto> seats, String hallName, String locationName, RegionLocation regionLocation) {
		this.id = id;
		this.seatLayout = seatLayout;
		this.seats = seats;
		this.hallName = hallName;
		this.locationName = locationName;
		this.regionLocation = regionLocation;
	}

}
