package org.codenbug.event.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.Getter;

@Embeddable
@Getter
public class Location {
	@Column(name = "location", nullable = false)
	private String value;

	protected Location() {}

	public Location(String value) {
		this.value = value;
	}
	protected void validate(){
		if ( value == null || value.isEmpty() )
			throw new IllegalArgumentException("location cannot be empty");
	}
}
