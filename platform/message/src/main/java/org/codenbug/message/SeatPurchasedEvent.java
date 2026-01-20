package org.codenbug.message;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class SeatPurchasedEvent {
	private String eventId;
	private List<String> seatIds;
	private Long seatLayoutId;
	private String userId;

}
