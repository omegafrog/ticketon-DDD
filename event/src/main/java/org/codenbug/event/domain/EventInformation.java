package org.codenbug.event.domain;

import java.time.LocalDateTime;

import org.codenbug.event.global.NewEventRequest;
import org.codenbug.event.global.UpdateEventRequest;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Lob;
import lombok.Getter;

/**
 * 유저에게 노출되는 event 객체의 정보가 포함된 객체
 */
@Embeddable
@Getter
public class EventInformation {
	@Column(name = "title", nullable = false)
	private String title;
	@Column(name = "thumbnail_url", nullable = false)
	private String thumbnailUrl;
	@Column(name = "ageLimit")
	private Integer ageLimit;
	@Lob
	@Column(name = "restrictions")
	private String restrictions;
	@Lob
	@Column(name = "description", nullable = false)
	private String description;
	@Column(name = "booking_start", nullable = false)
	private LocalDateTime bookingStart;
	@Column(name = "booking_end", nullable = false)
	private LocalDateTime bookingEnd;
	@Column(name = "event_start", nullable = false)
	private LocalDateTime eventStart;
	@Column(name = "event_end", nullable = false)
	private LocalDateTime eventEnd;
	@Column(name = "view_count", columnDefinition = "integer default 0", nullable = false)
	private Integer viewCount;
	@Column(name = "seat_selectable", columnDefinition = "boolean default false", nullable = false)
	private Boolean seatSelectable;

	@Enumerated(EnumType.STRING)
	private EventStatus status;
	@Embedded
	private EventCategoryId categoryId;

	protected EventInformation() {
	}

	public EventInformation(String title, String thumbnailUrl, Integer ageLimit, String restrictions,
		String description,
		LocalDateTime bookingStart, LocalDateTime bookingEnd, LocalDateTime eventStart, LocalDateTime eventEnd,
		Boolean seatSelectable, EventStatus status, EventCategoryId categoryId) {
		this.title = title;
		this.thumbnailUrl = thumbnailUrl;
		this.restrictions = restrictions;
		this.description = description;
		this.bookingStart = bookingStart;
		this.bookingEnd = bookingEnd;
		this.eventStart = eventStart;
		this.eventEnd = eventEnd;
		this.ageLimit = ageLimit;
		this.viewCount = 0;
		this.status = status == null ? EventStatus.CLOSED : status;
		this.seatSelectable = seatSelectable != null && seatSelectable;
		this.categoryId = categoryId;
		validate();
	}

	public EventInformation(NewEventRequest request) {
		this(request.getTitle(), request.getThumbnailUrl(),
			request.getAgeLimit(),
			request.getRestriction(),
			request.getDescription(), request.getBookingStart(), request.getBookingEnd(), request.getStartDate(),
			request.getEndDate(),
			request.isSeatSelectable(), EventStatus.CLOSED,
			request.getCategoryId());
	}

	// public EventInformation(EventInformation original, UpdateEventRequest request) {
	// 	this(request.getTitle(), request.getThumbnailUrl(), request.getAgeLimit(), request.getRestriction(), request.getDescription(),
	// 		request.getBookingStart(), request.getBookingEnd(), request.getStartDate(), request.getEndDate(),
	// 		request.getSeatSelectable(), request.getStatus(), original.getCategoryId() );
	// }

	public EventInformation applyChange(UpdateEventRequest request){
		this.title = request.getTitle() == null ? this.title : request.getTitle();
		this.thumbnailUrl = request.getThumbnailUrl() == null ? thumbnailUrl : request.getThumbnailUrl();
		this.ageLimit = request.getAgeLimit() == null? this.ageLimit : request.getAgeLimit();
		this.restrictions = request.getRestriction() == null ? this.restrictions : request.getRestriction();
		this.description = request.getDescription() == null ? this.description : request.getDescription();
		this.eventStart = request.getStartDate() == null ? this.eventStart : request.getStartDate();
		this.eventEnd = request.getEndDate() == null ? this.eventEnd : request.getEndDate();
		this.bookingStart = request.getBookingStart() == null ? this.bookingStart : request.getBookingStart();
		this.bookingEnd	= request.getBookingEnd() == null ? this.bookingEnd : request.getBookingEnd();
		this.status = request.getStatus() == null ? this.status : request.getStatus();
		this.seatSelectable = request.getSeatSelectable() == null ? this.seatSelectable : request.getSeatSelectable();
		return this;
	}

	/**
	 * parameter binding으로 생성된 경우 빈 생성자를 호출하므로 validate를 명시적으로 호출해 줘야 함
	 */
	protected void validate() {
		validateStringColumn();
		validateNumericColumn();
		validateBookingNEventDate();
		categoryId.validate();
	}

	private void validateNumericColumn() {
		if (ageLimit != null && ageLimit < 0)
			throw new IllegalStateException("age limit must be greater than 0");
	}

	private void validateStringColumn() {
		if (title == null || title.isEmpty() ||
			thumbnailUrl == null || thumbnailUrl.isEmpty() ||
			description == null || description.isEmpty())
			throw new IllegalStateException("value must not be null or empty");
	}

	private void validateBookingNEventDate() {
		if (bookingStart == null || bookingEnd == null || eventStart == null || eventEnd == null)
			throw new IllegalStateException("datetime must not be null or empty");

		if (bookingStart.isAfter(bookingEnd) || LocalDateTime.now().isAfter(bookingStart))
			throw new IllegalStateException("bookingStart must be after bookingEnd or now");

		if (eventStart.isAfter(eventEnd) || LocalDateTime.now().isAfter(eventStart))
			throw new IllegalStateException("eventStart must be after eventEnd or now");

		if (bookingEnd.isAfter(eventStart))
			throw new IllegalStateException("booking date must be before than event date");
	}

}
