package org.codenbug.event.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.Getter;

@Embeddable
@Getter
public class EventCategoryId {
	@Column(name = "event_category_id")
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
