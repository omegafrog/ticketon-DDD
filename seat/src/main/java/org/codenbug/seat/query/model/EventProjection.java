package org.codenbug.seat.query.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Getter;

@Entity(name = "seat-event-projection")
@Getter
public class EventProjection {

	@Id
	private String eventId;
	private Boolean seatSelectable;

}
