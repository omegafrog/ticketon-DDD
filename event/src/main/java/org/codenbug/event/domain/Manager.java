package org.codenbug.event.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.Getter;

@Embeddable
@Getter
public class Manager {
	@Column(name = "manager_id")
	private String managerId;

	protected Manager() {}
	public Manager(String managerId) {
		this.managerId = managerId;
	}
}
