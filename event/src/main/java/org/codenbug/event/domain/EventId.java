package org.codenbug.event.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.Getter;

@Embeddable
@Getter
public class EventId {

	@Column(name = "id")
	private String eventId;

	protected EventId() {}

	public EventId(String eventId) {
		this.eventId = eventId;
	}
}
