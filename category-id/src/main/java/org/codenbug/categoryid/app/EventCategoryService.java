package org.codenbug.categoryid.app;

import java.util.List;

import org.codenbug.categoryid.domain.CategoryId;
import org.codenbug.categoryid.domain.EventCategory;
import org.codenbug.categoryid.domain.EventCategoryRepository;
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

}
