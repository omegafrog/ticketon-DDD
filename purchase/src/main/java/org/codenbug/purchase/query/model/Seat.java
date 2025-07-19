package org.codenbug.purchase.query.model;

import jakarta.persistence.Embeddable;
import lombok.Getter;

@Embeddable
@Getter
public class Seat {
	private String signature;
	private int amount;
	private String grade;
	private String seatId;

	protected Seat() {
	}

	public Seat(String seatId, String signature, int amount, String grade) {
		this.seatId = seatId;
		this.signature = signature;
		this.amount = amount;
		this.grade = grade;
	}
}
