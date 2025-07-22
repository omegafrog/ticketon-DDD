package org.codenbug.event.global;

import java.io.Serializable;
import java.time.LocalDateTime;

import org.codenbug.event.domain.Event;
import org.codenbug.event.domain.EventId;
import org.codenbug.event.domain.EventStatus;
import org.codenbug.seat.global.SeatLayoutResponse;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Value;

@Getter
@NoArgsConstructor
public class EventInfoResponse implements Serializable {

	SeatLayoutResponse seatLayout;
	EventId eventId;
	String category;
	EventInfoDto information;
	LocalDateTime bookingStart;
	LocalDateTime bookingEnd;
	Integer viewCount;
	LocalDateTime createdAt;
	LocalDateTime modifiedAt;
	EventStatus status;
	Boolean seatSelectable;
	Boolean isDeleted;
	int maxPrice;
	int minPrice;

	public EventInfoResponse(Event event, String category, int maxPrice, int minPrice, SeatLayoutResponse seatLayout) {
		this.eventId = event.getEventId();
		this.category = category;
		this.maxPrice = maxPrice;
		this.minPrice = minPrice;
		this.information = new EventInfoDto(
			event.getEventInformation().getTitle(),
			event.getEventInformation().getThumbnailUrl(),
			event.getEventInformation().getDescription(),
			event.getEventInformation().getAgeLimit(),
			event.getEventInformation().getRestrictions(),
			seatLayout.getLocationName(),
			seatLayout.getHallName(),
			event.getEventInformation().getEventStart(),
			event.getEventInformation().getEventEnd(),
			seatLayout.getSeats().size()
		);
		this.bookingStart = event.getEventInformation().getBookingStart();
		this.bookingEnd = event.getEventInformation().getBookingEnd();
		this.viewCount = event.getEventInformation().getViewCount();
		this.createdAt = event.getMetaData().getCreatedAt();
		this.modifiedAt = event.getMetaData().getModifiedAt();
		this.status = event.getEventInformation().getStatus();
		this.seatSelectable = event.getEventInformation().getSeatSelectable();
		this.isDeleted = event.getMetaData().getDeleted();
		this.seatLayout = seatLayout;

	}

	/**
	 * DTO for {@link EventInfoResponse}
	 */
	@Value
	public static class EventInfoDto implements Serializable {
		String title;
		String thumbnailUrl;
		String description;
		Integer ageLimit;
		String restrictions;
		String location;
		String hallName;
		LocalDateTime eventStart;
		LocalDateTime eventEnd;
		Integer seatCount;
	}
}