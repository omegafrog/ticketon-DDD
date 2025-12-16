package org.codenbug.event.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.Getter;

@Embeddable
@Getter
@Valid
public class EventCategoryId {
	@Column(name = "event_category_id")
	@Min(value = -1, message = "Event category id는 0 이상의 값입니다.")
	private Long value;

	protected EventCategoryId() {}

	public EventCategoryId(Long value) {
		this.value = value;
	}

	protected void validate(){
		if( value == null || value < 0 )
			throw new IllegalStateException("value is null or negative");
	}
}
