package org.codenbug.event.global;

import lombok.Getter;

@Getter
public class SeatDto {
	private String signature;
	private String grade;
	private int price;

	protected SeatDto() {}

	public SeatDto(String signature, String grade, int price) {
		this.signature = signature;
		this.grade = grade;
		this.price = price;
	}
}
