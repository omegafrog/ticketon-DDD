package org.codenbug.event.ui;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.codenbug.common.exception.ControllerParameterValidationFailedException;
import org.codenbug.common.exception.FieldError;
import org.codenbug.event.domain.EventCategoryId;
import org.codenbug.seat.global.RegisterSeatLayoutDto;
import org.hibernate.validator.constraints.Length;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

@Getter
@Valid
public class NewEventRequest {
	@Length(min = 2, max = 255, message = "제목은 2자 이상 255자 이하여야 합니다.")
	private String title;
	private EventCategoryId categoryId;
	@NotBlank(message = "설명은 필수입니다.")
	private String description;
	private String restriction;
	private String thumbnailUrl;
	@NotNull(message = "좌석 레이아웃은 필수입니다.")
	private RegisterSeatLayoutDto seatLayout;
	@FutureOrPresent(message = "시작일은 현재 또는 미래의 날짜여야 합니다.")
	private LocalDateTime startDate;
	@Future(message = "종료일은 현재 이후의 날짜여야 합니다.")
	private LocalDateTime endDate;
	@FutureOrPresent(message = "예약 시작일은 현재 또는 미래의 날짜여야 합니다.")
	private LocalDateTime bookingStart;
	@Future(message = "예약 종료일은 현재 이후의 날짜여야 합니다.")
	private LocalDateTime bookingEnd;
	@Min(value = 0, message = "나이 제한은 0보다 크거나 같아야 합니다.")
	private int ageLimit;
	private boolean seatSelectable;
	@Min(value = 0, message = "가격은 0보다 크거나 같아야 합니다.")
	private int minPrice;
	@Min(value = 0, message = "가격은 0보다 크거나 같아야 합니다.")
	private int maxPrice;


	protected NewEventRequest() {}

	public NewEventRequest(String title, EventCategoryId categoryId, String description,
			String restriction, String thumbnailUrl, LocalDateTime startDate, LocalDateTime endDate,
			RegisterSeatLayoutDto seatLayout, LocalDateTime bookingStart, LocalDateTime bookingEnd,
			int ageLimit, boolean seatSelectable) {
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
		this.minPrice =
				seatLayout.getSeats().stream().map(seat -> seat.getPrice()).min((a, b) -> a - b).orElse(0);
		this.maxPrice =
				seatLayout.getSeats().stream().map(seat -> seat.getPrice()).max((a, b) -> a - b).orElse(0);
		validate();
	}

	private void validate() {
		List<FieldError> fieldErrors = new ArrayList<>();
		if (startDate.isAfter(endDate)) {
			fieldErrors
					.add(new FieldError("startDate", startDate.toString(), "startDate는 endDate 이전이어야 합니다."));
		}
		if (bookingStart.isAfter(bookingEnd)) {
			fieldErrors.add(new FieldError("bookingStart", bookingStart.toString(),
					"bookingStart는 bookingEnd 이전이어야 합니다"));
		}
		if (minPrice > maxPrice) {
			fieldErrors.add(new FieldError("minPrice", String.valueOf(minPrice),
					"minPrice는 maxPrice보다 작거나 같아야 합니다."));
		}
		if (!fieldErrors.isEmpty()) {
			throw new ControllerParameterValidationFailedException("파라미터 validation 실패했습니다.",
					fieldErrors);
		}
	}

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

	public RegisterSeatLayoutDto getSeatLayout() {
		return seatLayout;
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

	public int getAgeLimit() {
		return ageLimit;
	}

	public boolean isSeatSelectable() {
		return seatSelectable;
	}

	public int getMinPrice() {
		return minPrice;
	}

	public int getMaxPrice() {
		return maxPrice;
	}
}
