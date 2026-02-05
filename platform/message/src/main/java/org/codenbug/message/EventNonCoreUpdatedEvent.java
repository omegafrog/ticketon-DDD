package org.codenbug.message;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class EventNonCoreUpdatedEvent {
	public static final String TOPIC = "event-noncore-updated";
	private String eventId;
	private String managerId;
	private String title;
	private String occurredAt;
}
