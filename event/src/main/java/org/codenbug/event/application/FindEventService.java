package org.codenbug.event.application;

import org.codenbug.event.domain.EventRepository;
import org.codenbug.seat.app.FindSeatLayoutService;
import org.springframework.stereotype.Service;

@Service
public class FindEventService {
	private final EventRepository repository;
	private final FindSeatLayoutService findSeatLayoutService;

	public FindEventService(EventRepository repository, FindSeatLayoutService findSeatLayoutService) {
		this.repository = repository;
		this.findSeatLayoutService = findSeatLayoutService;
	}
}
