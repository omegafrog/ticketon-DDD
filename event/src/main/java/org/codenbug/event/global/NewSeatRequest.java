package org.codenbug.event.global;

import java.util.List;

import org.codenbug.seat.global.SeatDto;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class NewSeatRequest {
	private String hallName;
	private int seatCount;
	private String location;
	private String layout;
	private List<SeatDto> seats;

	public NewSeatRequest(String hallName, String location, String seatLayout, List<SeatDto> seats) {
		this.hallName = hallName;
		this.seatCount = seats.size();
		this.location = location;
		this.layout = seatLayout;
		this.seats = seats;
	}
}
