package org.codenbug.event.category.domain;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import lombok.Getter;

@Entity
@Getter
public class EventCategory {
	@EmbeddedId
	private CategoryId id;
	private String name;
	private String thumbnailUrl;

	protected EventCategory() {
	}

	public EventCategory(CategoryId id, String name, String thumbnailUrl) {
		this.id = id;
		this.name = name;
		this.thumbnailUrl = thumbnailUrl;
	}
}
