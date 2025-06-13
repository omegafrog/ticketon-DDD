package org.codenbug.event.global;

import java.time.LocalDateTime;

import org.codenbug.event.domain.EventStatus;

import lombok.Getter;

@Getter
public class UpdateEventRequest {
	private String title;
	private String description;
	private String restriction;
	private String thumbnailUrl;
	private LocalDateTime startDate;
	private LocalDateTime endDate;
	private LocalDateTime bookingStart;
	private LocalDateTime bookingEnd;
	private EventStatus status;
	private Integer ageLimit;
	private Boolean seatSelectable;

	protected UpdateEventRequest() {}
}
