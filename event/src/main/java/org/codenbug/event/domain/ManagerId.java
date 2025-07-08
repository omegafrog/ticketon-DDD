package org.codenbug.event.domain;

import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.Getter;

@Embeddable
@Getter
public class ManagerId {
	@Column(name = "manager_id")
	private String managerId;

	protected ManagerId() {}
	public ManagerId(String managerId) {
		this.managerId = managerId;
	}

	@Override
	public boolean equals(Object o) {
		if (!(o instanceof ManagerId managerId1))
			return false;
		return Objects.equals(managerId, managerId1.managerId);
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(managerId);
	}
}
