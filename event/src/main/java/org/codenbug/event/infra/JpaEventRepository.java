package org.codenbug.event.infra;

import org.codenbug.event.domain.Event;
import org.codenbug.event.domain.EventId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaEventRepository extends JpaRepository<Event, EventId> {
}
