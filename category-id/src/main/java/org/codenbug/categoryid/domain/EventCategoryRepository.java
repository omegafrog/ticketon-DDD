package org.codenbug.categoryid.domain;

import java.util.List;
import java.util.Optional;


public interface EventCategoryRepository {
	Optional<EventCategory> findById(CategoryId categoryId);

	List<EventCategory> findAll(List<Long> ids);

}
