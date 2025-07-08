package org.codenbug.seat.domain;

import com.fasterxml.uuid.Generators;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import jakarta.persistence.EmbeddedId;
import lombok.Getter;

@Embeddable
@Getter
public class Seat {
	@Embedded
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
		return new SeatId(Generators.timeBasedEpochGenerator().generate().toString());
	}
}
