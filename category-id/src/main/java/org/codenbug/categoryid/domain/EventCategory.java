package org.codenbug.categoryid.domain;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;

@Entity
public class EventCategory {
	@EmbeddedId
	private CategoryId id;
	private String name;
	private String thumbnailUrl;
}
