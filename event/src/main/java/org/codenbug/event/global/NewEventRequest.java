package org.codenbug.event.global;

import java.time.LocalDateTime;

import org.codenbug.event.domain.EventCategoryId;
import org.codenbug.seat.global.SeatLayoutDto;

import lombok.Getter;

@Getter
public class NewEventRequest {
	private String title;
	private EventCategoryId categoryId;
	private String description;
	private String restriction;
	private String thumbnailUrl;
	private SeatLayoutDto seatLayout;
	private LocalDateTime startDate;
	private LocalDateTime endDate;
	private LocalDateTime bookingStart;
	private LocalDateTime bookingEnd;
	private int ageLimit;
	private boolean seatSelectable;


	protected NewEventRequest() {
	}

	public NewEventRequest(String title, EventCategoryId categoryId, String description, String restriction,
		String thumbnailUrl, LocalDateTime startDate, LocalDateTime endDate, SeatLayoutDto seatLayout,
		LocalDateTime bookingStart, LocalDateTime bookingEnd, int ageLimit, boolean seatSelectable) {
		this.title = title;
		this.categoryId = categoryId;
		this.description = description;
		this.restriction = restriction;
		this.thumbnailUrl = thumbnailUrl;
		this.startDate = startDate;
		this.endDate = endDate;
		this.seatLayout = seatLayout;
		this.bookingStart = bookingStart;
		this.bookingEnd = bookingEnd;
		this.ageLimit = ageLimit;
		this.seatSelectable = seatSelectable;
	}
}
