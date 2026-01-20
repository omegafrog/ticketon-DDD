package org.codenbug.purchase.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class SeatInfo {
	private final String seatId;
	private final String signature;
	private final String grade;
	private final int price;
	private final boolean available;
}
