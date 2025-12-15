package org.codenbug.seat.global;

import org.codenbug.seat.domain.Seat;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

@Getter
@Valid
public class SeatDto {
	private String id;
	@NotBlank
	private String signature;
	@NotBlank
	private String grade;
	@Min(value = 0, message = "가격은 0 이상이어야 합니다.")
	private int price;
	private boolean available;

	protected SeatDto() {}
	public SeatDto(String id, String signature, String grade, int price, boolean available) {
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
