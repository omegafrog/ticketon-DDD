package org.codenbug.event.global;

import java.time.LocalDateTime;

import org.codenbug.event.domain.EventCategoryId;
import org.codenbug.event.domain.EventStatus;

import lombok.Getter;

@Getter
public class EventManagerListResponse {
    private final String eventId;
    private final String title;
    private final EventCategoryId category;
    private final String thumbnailUrl;
    private final EventStatus status;
    private final LocalDateTime startDate;
    private final LocalDateTime endDate;
    private final String location;
    private final String hallName;
    private final boolean isDeleted;
    private final LocalDateTime bookingStart;
    private final LocalDateTime bookingEnd;

    public EventManagerListResponse(
        String eventId,
        String title,
        EventCategoryId category,
        String thumbnailUrl,
        EventStatus status,
        LocalDateTime startDate,
        LocalDateTime endDate,
        String location,
        String hallName,
        boolean isDeleted,
        LocalDateTime bookingStart,
        LocalDateTime bookingEnd
    ) {
        this.eventId = eventId;
        this.title = title;
        this.category = category;
        this.thumbnailUrl = thumbnailUrl;
        this.status = status;
        this.startDate = startDate;
        this.endDate = endDate;
        this.location = location;
        this.hallName = hallName;
        this.isDeleted = isDeleted;
        this.bookingStart = bookingStart;
        this.bookingEnd = bookingEnd;
    }
}