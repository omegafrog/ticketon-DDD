package org.codenbug.message;

import java.util.List;

import lombok.Getter;

@Getter
public class SeatPurchasedCanceledEvent {
	private List<String> seatIds;
	private String purchaseId;

	protected SeatPurchasedCanceledEvent(){}

	public SeatPurchasedCanceledEvent(List<String> seatIds, String purchaseId) {
		this.seatIds = seatIds;
		this.purchaseId = purchaseId;
	}

}
