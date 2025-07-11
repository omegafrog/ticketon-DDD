package org.codenbug.seat.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.Getter;

@Embeddable
@Getter
public class Location {
	@Column(name = "location", nullable = false)
	private String locationName;

	@Column(name = "hallName", nullable = false)
	private String hallName;

	protected Location() {
	}

	public Location(String locationName, String hallName) {
		this.locationName = locationName;
		this.hallName = hallName;
	}
	protected void validate(){
		if ( locationName == null || locationName.isEmpty() )
			throw new IllegalArgumentException("location cannot be empty");
		if ( hallName == null || hallName.isEmpty() )
			throw new IllegalArgumentException("hallName cannot be empty");
	}
}
