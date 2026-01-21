package org.codenbug.seat.domain;

import org.codenbug.common.Util;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.Getter;

@Entity
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
	private boolean available;

	@ManyToOne
	@JoinColumn(name ="layout_id")
	private SeatLayout seatLayout;

	protected Seat() {}

	public Seat(String signature, int amount, String grade) {
		this.seatId = generateSeatId();
		this.grade = grade;
		this.signature = signature;
		this.amount = amount;
		this.available = true;
	}
	private SeatId generateSeatId() {
		return new SeatId(Util.ID.createUUID());
	}
	public Seat updateTarget(SeatLayout target){
		this.seatLayout = target;
		return this;
	}

	public void setAvailable(boolean b) {
		this.available = b;
	}

	public void reserve() {
		this.available=false;
	}

	public void cancelReserve() {
		this.available=true;
	}
}
