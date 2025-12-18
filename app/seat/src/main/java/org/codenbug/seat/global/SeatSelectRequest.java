package org.codenbug.seat.global;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class SeatSelectRequest {
	private List<String> seatList;
	private Integer ticketCount;
}