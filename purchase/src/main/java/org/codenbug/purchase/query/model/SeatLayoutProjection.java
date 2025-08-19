package org.codenbug.purchase.query.model;

import java.util.Set;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import lombok.Getter;

@Entity
@Getter
public class SeatLayoutProjection {
	@Id
	private Long layoutId;
	private String layout;
	private String locationName;
	private String hallName;
	@ElementCollection
	@CollectionTable(name = "seat_layout_projection_seats", joinColumns = @JoinColumn(name = "layout_id"))
	private Set<Seat> seats;

	protected SeatLayoutProjection(){}

	public SeatLayoutProjection(Long layoutId, String layout, String locationName, String hallName, Set<Seat> seats){
		this.layoutId = layoutId;
		this.layout = layout;
		this.locationName = locationName;
		this.hallName = hallName;
		this.seats = seats;
	}

	public void updateFrom(String layout, String locationName, String hallName, Set<Seat> seats) {
		this.layout = layout;
		this.locationName = locationName;
		this.hallName = hallName;
		this.seats = seats;
	}
}
