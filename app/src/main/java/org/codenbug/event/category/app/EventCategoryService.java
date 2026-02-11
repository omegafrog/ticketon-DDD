package org.codenbug.event.category.app;

import java.util.List;

import org.codenbug.event.category.domain.CategoryId;
import org.codenbug.event.category.domain.EventCategory;
import org.codenbug.event.category.domain.EventCategoryRepository;
import org.codenbug.event.category.global.EventCategoryListResponse;
import org.springframework.stereotype.Service;

import jakarta.persistence.EntityNotFoundException;

@Service
public class EventCategoryService {
	private final EventCategoryRepository eventCategoryRepository;

	public EventCategoryService(EventCategoryRepository eventCategoryRepository) {
		this.eventCategoryRepository = eventCategoryRepository;
	}

	public void validateExist(Long eventCategoryId) {
		eventCategoryRepository.findById(new CategoryId(eventCategoryId))
			.orElseThrow(() -> new EntityNotFoundException("Cannot find event category"));
	}


	public List<EventCategory> findAllByIds(List<Long> ids) {
		return eventCategoryRepository.findAll(ids);
	}

	public EventCategory findById(Long id) {
		return eventCategoryRepository.findById(new CategoryId(id))
			.orElseThrow(() -> new EntityNotFoundException());
	}

	public List<EventCategoryListResponse> getAllCategories() {
		List<EventCategory> categories = eventCategoryRepository.findAll();
		return categories.stream()
			.map(category -> new EventCategoryListResponse(
				category.getId().getId(),
				category.getName()
			))
			.toList();
	}
}
