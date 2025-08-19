package org.codenbug.event.domain;

import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.Getter;

@Embeddable
@Getter
public class SeatLayoutId {
	@Column(name = "seat_layout_id", nullable = true)
	private Long value;

	protected SeatLayoutId() {}
	public SeatLayoutId(Long value) {
		this.value = value;
	}

	@Override
	public boolean equals(Object o) {
		if (!(o instanceof SeatLayoutId that))
			return false;
		return Objects.equals(value, that.value);
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(value);
	}
}
