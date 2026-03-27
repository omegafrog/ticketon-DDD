package org.codenbug.seat.global;

import java.util.List;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class SeatSelectRequest {
	@NotEmpty
	private List<String> seatList;

	@NotNull
	@Min(1)
	private Integer ticketCount;
}
