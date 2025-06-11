package org.codenbug.event.global;

import java.util.List;

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
	private boolean seatSelectable;

	public NewSeatRequest(String hallName, String location, String seatLayout, List<SeatDto> seats,
		boolean seatSelectable) {
		this.hallName = hallName;
		this.seatCount = seats.size();
		this.location = location;
		this.layout = seatLayout;
		this.seats = seats;
		this.seatSelectable = seatSelectable;
	}
}
