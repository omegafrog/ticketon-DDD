package org.codenbug.seat.global;

import org.codenbug.seat.domain.Seat;

import lombok.Getter;

@Getter
public class SeatDto {
	private String id;
	private String signature;
	private String grade;
	private Integer price;
	private boolean available;

	protected SeatDto() {}

	public SeatDto(String id, String signature, String grade, Integer price, boolean available) {
		this.id = id;
		this.signature = signature;
		this.grade = grade;
		this.price = price;
		this.available = available;
	}

	public SeatDto(Seat seat) {
		this(seat.getSeatId().getValue(), seat.getSignature(), seat.getGrade(), seat.getAmount(), seat.isAvailable());
	}
}
