package org.codenbug.seat.global;

import java.util.List;

import lombok.Getter;

@Getter
public class RegisterSeatLayoutDto {
	private List<List<String>> layout;
	private List<SeatDto> seats;
	private String location;
	private String hallName;

	protected RegisterSeatLayoutDto() {
	}

	public RegisterSeatLayoutDto(List<List<String>> layout, List<SeatDto> seats, String location, String hallName) {
		this.layout = layout;
		this.seats = seats;
		this.location = location;
		this.hallName = hallName;
	}


}
