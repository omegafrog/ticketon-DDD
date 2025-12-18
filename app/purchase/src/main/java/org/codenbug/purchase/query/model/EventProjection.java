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
	private String managerId;
	private Long seatLayoutId;
	private boolean seatSelectable;
	private String location;
	private String startTime;
	private String endTime;
	private Long version;
	private String status;


	protected EventProjection(){}

	public EventProjection(String eventId, String title, String managerId, Long seatLayoutId, boolean seatSelectable, String location, String startTime, String endTime, Long version, String status) {
		this.eventId = eventId;
		this.title = title;
		this.managerId = managerId;
		this.seatLayoutId = seatLayoutId;
		this.seatSelectable = seatSelectable;
		this.location = location;
		this.startTime = startTime;
		this.endTime = endTime;
		this.version = version;
		this.status = status;
	}
	
	public void updateFrom(String title, String managerId, Long seatLayoutId, boolean seatSelectable, String location, String startTime, String endTime, Long version, String status) {
		this.title = title;
		this.managerId = managerId;
		this.seatLayoutId = seatLayoutId;
		this.seatSelectable = seatSelectable;
		this.location = location;
		this.startTime = startTime;
		this.endTime = endTime;
		this.version = version;
		this.status = status;
	}
}
