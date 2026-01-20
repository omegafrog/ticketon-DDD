package org.codenbug.purchase.domain;

import java.util.List;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class SeatLayoutInfo {
	private final Long layoutId;
	private final String locationName;
	private final String hallName;
	private final List<SeatInfo> seats;
}
