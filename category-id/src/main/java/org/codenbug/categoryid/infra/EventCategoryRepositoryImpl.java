package org.codenbug.categoryid.infra;

import java.util.Optional;

import org.codenbug.categoryid.domain.CategoryId;
import org.codenbug.categoryid.domain.EventCategory;
import org.codenbug.categoryid.domain.EventCategoryRepository;
import org.springframework.stereotype.Repository;

@Repository
public class EventCategoryRepositoryImpl implements EventCategoryRepository {
	private final JpaEventCategoryRepository repository;

	public EventCategoryRepositoryImpl(JpaEventCategoryRepository repository) {
		this.repository = repository;
	}

	@Override
	public Optional<EventCategory> findById(CategoryId categoryId) {
		return repository.findById(categoryId);
	}
}
