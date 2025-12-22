package org.codenbug.event.domain;

import org.codenbug.event.application.dto.EventListFilter;
import org.codenbug.event.application.dto.response.EventInfoResponse;
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
