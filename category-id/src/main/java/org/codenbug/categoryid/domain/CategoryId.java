package org.codenbug.categoryid.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.Getter;

@Embeddable
@Getter
public class CategoryId {
	@Column(name = "id")
	private Long id;

	protected CategoryId(){}

	public CategoryId(Long id) {
		this.id = id;
	}
}
