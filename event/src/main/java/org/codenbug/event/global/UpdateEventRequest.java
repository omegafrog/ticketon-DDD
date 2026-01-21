package org.codenbug.event.global;

import java.time.LocalDateTime;

import org.codenbug.event.domain.EventCategoryId;
import org.codenbug.event.domain.EventStatus;
import org.codenbug.seat.global.RegisterSeatLayoutDto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Getter;

@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public class UpdateEventRequest {
	private String title;
	private EventCategoryId categoryId;
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
	private RegisterSeatLayoutDto seatLayout;

	protected UpdateEventRequest() {}
}
