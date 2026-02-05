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

	// NOTE: Explicit getters are kept to support tooling that does not process Lombok.
	public String getTitle() {
		return title;
	}

	public EventCategoryId getCategoryId() {
		return categoryId;
	}

	public String getDescription() {
		return description;
	}

	public String getRestriction() {
		return restriction;
	}

	public String getThumbnailUrl() {
		return thumbnailUrl;
	}

	public LocalDateTime getStartDate() {
		return startDate;
	}

	public LocalDateTime getEndDate() {
		return endDate;
	}

	public LocalDateTime getBookingStart() {
		return bookingStart;
	}

	public LocalDateTime getBookingEnd() {
		return bookingEnd;
	}

	public EventStatus getStatus() {
		return status;
	}

	public Integer getAgeLimit() {
		return ageLimit;
	}

	public Boolean getSeatSelectable() {
		return seatSelectable;
	}

	public RegisterSeatLayoutDto getSeatLayout() {
		return seatLayout;
	}
}
