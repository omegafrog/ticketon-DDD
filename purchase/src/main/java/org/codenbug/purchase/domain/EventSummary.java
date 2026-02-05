package org.codenbug.purchase.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class EventSummary {
	private final String eventId;
	private final Long seatLayoutId;
	private final boolean seatSelectable;
	private final String status;
	private final Long version;
	private final Long salesVersion;
	private final String title;
}
