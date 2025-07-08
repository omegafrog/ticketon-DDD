package org.codenbug.seat.global;

import java.util.List;

import lombok.Getter;

@Getter
public class SeatLayoutDto {
	private List<List<String>> layout;

	private List<SeatDto> seats;

	protected SeatLayoutDto() {
	}

	public SeatLayoutDto(List<List<String>> layout, List<SeatDto> seats) {
		this.layout = layout;
		this.seats = seats;

	}


}
