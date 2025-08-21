package org.codenbug.event.domain;

import java.util.Objects;

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

	@Override
	public boolean equals(Object o) {
		if (!(o instanceof EventId eventId1))
			return false;
		return Objects.equals(eventId, eventId1.eventId);
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(eventId);
	}
}
