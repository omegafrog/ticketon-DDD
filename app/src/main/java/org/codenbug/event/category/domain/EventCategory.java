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
}
