package org.codenbug.event.infra;

import java.time.LocalDateTime;

import org.codenbug.event.domain.Event;
import org.codenbug.event.domain.EventId;
import org.codenbug.event.domain.SeatLayoutId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface JpaEventRepository extends JpaRepository<Event, EventId>, JpaSpecificationExecutor<Event> {
	Event findBySeatLayoutId(SeatLayoutId seatLayoutId);

	@Modifying(clearAutomatically = true, flushAutomatically = true)
	@Query("update Event e set e.metaData.deleted = true, e.metaData.modifiedAt = :modifiedAt where e.eventId = :id")
	int markDeleted(@Param("id") EventId id, @Param("modifiedAt") LocalDateTime modifiedAt);
}
