package org.codenbug.event.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.Getter;

@Embeddable
@Getter
public class SeatPriceRange {
	@Column(name = "minPrice", nullable = false)
	private Integer minPrice;
	@Column(name = "maxPrice", nullable = false)
	private Integer maxPrice;

	protected SeatPriceRange() {}

	public SeatPriceRange(Integer minPrice, Integer maxPrice) {
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
