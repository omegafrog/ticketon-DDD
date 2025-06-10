package org.codenbug.event.domain;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Lob;
import lombok.Setter;

/**
 * 유저에게 노출되는 event 객체의 정보가 포함된 객체
 */
@Embeddable
public class EventInformation {
	@Column(name = "title", nullable = false)
	private String title;
	@Column(name = "thumbnailUrl", nullable = false)
	private String thumbnailUrl;
	@Column(name = "ageLimit")
	private Integer ageLimit;
	@Lob
	@Column(name = "restrictions")
	private String restrictions;
	@Lob
	@Column(name = "description", nullable = false)
	private String description;
	@Column(name = "bookingStart", nullable = false)
	private LocalDateTime bookingStart;
	@Column(name = "bookingEnd", nullable = false)
	private LocalDateTime bookingEnd;
	@Column(name = "eventStart", nullable = false)
	private LocalDateTime eventStart;
	@Column(name = "eventEnd", nullable = false)
	private LocalDateTime eventEnd;
	@Column(name = "viewCount", columnDefinition = "default 0", nullable = false)
	private Integer viewCount;
	@Column(name = "seatSelectable", nullable = false, columnDefinition = "default false")
	private Boolean seatSelectable;
	@Column(name = "hallName", nullable = false)
	private String hallName;
	@Column(name = "seatCount", nullable = false)
	@Setter
	private Integer seatCount;
	@Enumerated(EnumType.STRING)
	private EventStatus status;
	@Embedded
	private SeatPriceRange priceRange;
	@Embedded
	private EventCategoryId categoryId;
	@Embedded
	private Location location;

	protected EventInformation() {
	}

	public EventInformation(String title, String thumbnailUrl, Integer ageLimit, String restrictions,
		String description,
		LocalDateTime bookingStart, LocalDateTime bookingEnd, LocalDateTime eventStart, LocalDateTime eventEnd,
		Boolean seatSelectable, String hallName, Integer seatCount, EventStatus status,
		SeatPriceRange priceRange, EventCategoryId categoryId, Location location) {
		this.title = title;
		this.thumbnailUrl = thumbnailUrl;
		this.restrictions = restrictions;
		this.description = description;
		this.hallName = hallName;
		this.bookingStart = bookingStart;
		this.bookingEnd = bookingEnd;
		this.eventStart = eventStart;
		this.eventEnd = eventEnd;
		this.seatCount = seatCount;
		this.ageLimit = ageLimit;
		this.viewCount = 0;
		this.status = status == null ? EventStatus.OPEN : status;
		this.seatSelectable = seatSelectable != null && seatSelectable;
		this.categoryId = categoryId;
		this.priceRange = priceRange;
		this.location = location;
		validate();
	}

	private void validate() {
		validateStringColumn();
		validateNumericColumn();
		validateBookingNEventDate();
		categoryId.validate();
		priceRange.validate();
		location.validate();
	}

	private void validateNumericColumn() {
		if (ageLimit != null && ageLimit < 0)
			throw new IllegalStateException("age limit must be greater than 0");
		if (seatCount == null || seatCount < 0)
			throw new IllegalStateException("seat count must be greater than 0");
	}

	private void validateStringColumn() {
		if (title == null || title.isEmpty() ||
			thumbnailUrl == null || thumbnailUrl.isEmpty() ||
			description == null || description.isEmpty() ||
			hallName == null || hallName.isEmpty())
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
