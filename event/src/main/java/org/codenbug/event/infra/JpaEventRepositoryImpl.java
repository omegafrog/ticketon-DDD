package org.codenbug.event.infra;

import org.codenbug.event.domain.Event;
import org.codenbug.event.domain.EventId;
import org.codenbug.event.domain.EventRepository;
import org.springframework.stereotype.Repository;

@Repository
public class JpaEventRepositoryImpl implements EventRepository {

	private JpaEventRepository jpaEventRepository;

	public JpaEventRepositoryImpl(JpaEventRepository jpaEventRepository) {
		this.jpaEventRepository = jpaEventRepository;
	}
	protected JpaEventRepositoryImpl(){}

	@Override
	public Event save(Event event) {
		return jpaEventRepository.save(event);
	}

	@Override
	public Event findEvent(EventId id) {
		return jpaEventRepository.findById(id)
			.orElseThrow(() -> new RuntimeException("Cannot find event"));
	}
}
