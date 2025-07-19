package org.codenbug.seat.global;

import org.codenbug.seat.domain.Seat;

import lombok.Getter;

@Getter
public class SeatDto {
	private String signature;
	private String grade;
	private Integer price;

	protected SeatDto() {}

	public SeatDto(String signature, String grade, Integer price) {
		this.signature = signature;
		this.grade = grade;
		this.price = price;
	}

	public SeatDto(Seat seat) {
		this(seat.getSignature(), seat.getGrade(), seat.getAmount());
	}
}
