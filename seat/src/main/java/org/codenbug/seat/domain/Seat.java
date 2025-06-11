package org.codenbug.seat.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import lombok.Getter;

@Embeddable
@Getter
public class Seat {
	@EmbeddedId
	private SeatId seatId;
	@Column(name = "signature")
	private String signature;
	@Column(name = "grade")
	private String grade;
	@Column(name = "amount")
	private int amount;

	protected Seat() {}

	public Seat(String signature, int amount, String grade) {
		this.seatId = generateSeatId();
		this.grade = grade;
		this.signature = signature;
		this.amount = amount;
	}
	private SeatId generateSeatId() {
		throw new RuntimeException("Not implemented yet");
	}
}
