package org.codenbug.event.infra;

import org.codenbug.event.domain.Event;
import org.codenbug.event.domain.EventId;
import org.codenbug.event.domain.EventRepository;
import org.codenbug.event.global.EventInfoResponse;
import org.codenbug.event.global.EventListFilter;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

@Repository
public class EventRepositoryImpl implements EventRepository {

	private final JpaEventRepository jpaEventRepository;

	public EventRepositoryImpl(JpaEventRepository jpaEventRepository) {
		this.jpaEventRepository = jpaEventRepository;
	}

	@Override
	public Event save(Event event) {
		return jpaEventRepository.save(event);
	}

	@Override
	public Event findEvent(EventId id) {
		return jpaEventRepository.findById(id)
			.orElseThrow(() -> new RuntimeException("Cannot find event"));
	}
	@Override
	public Page<Event> getEventList(String keyword, EventListFilter filter, Pageable pageable) {

		// 1. 기본 조건을 직접 할당하여 Specification 체인을 시작합니다.
		//    이렇게 하면 deprecated된 where()를 사용하지 않게 됩니다.
		Specification<Event> spec = EventSpecification.isNotDeleted();

		// 2. 키워드가 있으면 키워드 검색 조건을 추가합니다.
		if (StringUtils.hasText(keyword)) {
			spec = spec.and(EventSpecification.titleContains(keyword));
		}

		// 3. 필터가 있으면 필터 조건을 추가합니다.
		if (filter != null) {
			// fromFilter가 null을 반환할 수 있으므로, null 체크 후 조합합니다.
			Specification<Event> filterSpec = EventSpecification.fromFilter(filter);
			if (filterSpec != null) {
				spec = spec.and(filterSpec);
			}
		}

		// 4. 조합된 Specification으로 데이터를 조회합니다.
		Page<Event> eventPage = jpaEventRepository.findAll(spec, pageable);

		// 5. Page<Event>를 Page<EventListResponse>로 변환하여 반환합니다.
		//    미완성된 생성자 호출을 올바르게 수정합니다.
		return eventPage;
	}


	@Override
	public EventInfoResponse getEventInfo(Long id) {
		return null;
	}
}
