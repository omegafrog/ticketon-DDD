package org.codenbug.message;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class SeatPurchasedCompleteEvent {
	private String userId;
}
