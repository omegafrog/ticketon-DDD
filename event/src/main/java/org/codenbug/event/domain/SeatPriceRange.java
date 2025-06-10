package org.codenbug.event.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.Getter;

@Embeddable
@Getter
public class SeatPriceRange {
	@Column(name = "minPrice", nullable = false)
	private double minPrice;
	@Column(name = "maxPrice", nullable = false)
	private double maxPrice;

	protected SeatPriceRange() {}

	public SeatPriceRange(double minPrice, double maxPrice) {
		if( minPrice > maxPrice) {
			throw new IllegalArgumentException("minPrice must be less than maxPrice");
		}
		this.minPrice = minPrice;
		this.maxPrice = maxPrice;
	}

	public void validate() {
		if(minPrice > maxPrice) {
			throw new IllegalArgumentException("minPrice must be less than maxPrice");
		}
	}
}
