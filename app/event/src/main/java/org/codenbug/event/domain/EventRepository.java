package org.codenbug.event.domain;

import org.codenbug.event.global.dto.response.EventInfoResponse;
import org.codenbug.event.global.dto.EventListFilter;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface EventRepository {

    Event save(Event event);

    Event findEvent(EventId id);

    Event findBySeatLayoutId(SeatLayoutId seatLayoutId);

    Page<Event> getEventList(String keyword, EventListFilter filter, Pageable pageable);

    Page<Event> getManagerEventList(ManagerId managerId, Pageable pageable);

    EventInfoResponse getEventInfo(Long id);
}
