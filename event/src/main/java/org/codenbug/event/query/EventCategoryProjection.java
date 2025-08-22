package org.codenbug.event.query;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Getter;

@Entity
@Getter
public class EventCategoryProjection {
	@Id
	private Long id;
	private String name;
	private String thumbnailUrl;

}
