package org.codenbug.event.global;

import lombok.Getter;

@Getter
public class EventSummaryResponse {
	private final String eventId;
	private final Long seatLayoutId;
	private final boolean seatSelectable;
	private final String status;
	private final Long version;
	private final Long salesVersion;
	private final String title;
	private final String managerId;

	public EventSummaryResponse(String eventId, Long seatLayoutId, boolean seatSelectable, String status, Long version,
		Long salesVersion, String title) {
		this(eventId, seatLayoutId, seatSelectable, status, version, salesVersion, title, null);
	}

	public EventSummaryResponse(String eventId, Long seatLayoutId, boolean seatSelectable, String status, Long version,
		Long salesVersion, String title, String managerId) {
		this.eventId = eventId;
		this.seatLayoutId = seatLayoutId;
		this.seatSelectable = seatSelectable;
		this.status = status;
		this.version = version;
		this.salesVersion = salesVersion;
		this.title = title;
		this.managerId = managerId;
	}
}
