package org.codenbug.purchase.query.model;


import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Getter;

@Entity
@Getter
public class EventProjection {

	@Id
	private String eventId;
	private String title;
	private String userId;
	private Long seatLayoutId;
	private boolean seatSelectable;


	protected EventProjection(){}

	public EventProjection(String eventId, String title, String userId, boolean seatSelectable, Long seatLayoutId) {
		this.seatLayoutId = seatLayoutId;
		this.eventId = eventId;
		this.title = title;
		this.userId = userId;
		this.seatSelectable = seatSelectable;
	}
}
