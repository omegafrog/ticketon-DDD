package org.codenbug.event.domain;

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
}
