package org.codenbug.event.ui;

import java.time.LocalDateTime;

import org.codenbug.event.domain.EventCategoryId;

import lombok.Getter;

@Getter
public class EventRegisterRequest {
	private String title;
	private EventCategoryId categoryId;
	private String description;
	private String restriction;
	private String thumbnailUrl;
	private LocalDateTime startDate;
	private LocalDateTime endDate;
	private String location;
	private String hallName;
	private LocalDateTime bookingStart;
	private LocalDateTime bookingEnd;
	private int ageLimit;

}
