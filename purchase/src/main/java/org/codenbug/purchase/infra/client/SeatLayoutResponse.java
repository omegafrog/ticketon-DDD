package org.codenbug.purchase.infra.client;

import java.util.List;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class SeatLayoutResponse {
	private Long id;
	private String seatLayout;
	private List<SeatDto> seats;
	private String hallName;
	private String locationName;
}
