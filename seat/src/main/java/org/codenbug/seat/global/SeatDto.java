package org.codenbug.seat.global;

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
}
