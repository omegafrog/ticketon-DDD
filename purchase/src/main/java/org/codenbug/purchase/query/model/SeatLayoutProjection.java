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
	private String location;
	@ElementCollection
	@CollectionTable(name = "seat_layout_projection_seats", joinColumns = @JoinColumn(name = "layout_id"))
	private Set<Seat> seats;

	protected SeatLayoutProjection(){}

	public SeatLayoutProjection(Long layoutId, String location, Set<Seat> seats){
		this.layoutId = layoutId;
		this.location = location;
		this.seats = seats;
	}


}
