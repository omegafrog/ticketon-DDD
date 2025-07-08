package org.codenbug.categoryid.domain;

import java.util.Optional;


public interface EventCategoryRepository {
	Optional<EventCategory> findById(CategoryId categoryId);
}
