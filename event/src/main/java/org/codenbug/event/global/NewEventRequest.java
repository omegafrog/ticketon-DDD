package org.codenbug.event.global;

import java.time.LocalDateTime;

import org.codenbug.event.domain.EventCategoryId;
import org.codenbug.seat.global.RegisterSeatLayoutDto;

import lombok.Getter;

@Getter
public class NewEventRequest {
	private String title;
	private EventCategoryId categoryId;
	private String description;
	private String restriction;
	private String thumbnailUrl;
	private RegisterSeatLayoutDto seatLayout;
	private LocalDateTime startDate;
	private LocalDateTime endDate;
	private LocalDateTime bookingStart;
	private LocalDateTime bookingEnd;
	private int ageLimit;
	private boolean seatSelectable;
	private int minPrice;
	private int maxPrice;


	protected NewEventRequest() {
	}

	public NewEventRequest(String title, EventCategoryId categoryId, String description, String restriction,
		String thumbnailUrl, LocalDateTime startDate, LocalDateTime endDate, RegisterSeatLayoutDto seatLayout,
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
		this.minPrice = seatLayout.getSeats().stream().map(seat -> seat.getPrice()).min((a, b) -> a - b).orElse(0);
		this.maxPrice = seatLayout.getSeats().stream().map(seat -> seat.getPrice()).max((a,b) ->a-b).orElse(0);
	}
}
