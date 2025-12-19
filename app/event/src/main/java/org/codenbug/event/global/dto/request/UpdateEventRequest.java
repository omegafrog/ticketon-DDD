package org.codenbug.event.global.dto.request;

import java.time.LocalDateTime;

import org.codenbug.event.domain.EventStatus;
import org.codenbug.seat.global.RegisterSeatLayoutDto;

import lombok.Getter;

@Getter
public class UpdateEventRequest {

    private String title;
    private String description;
    private String restriction;
    private String thumbnailUrl;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private LocalDateTime bookingStart;
    private LocalDateTime bookingEnd;
    private EventStatus status;
    private Integer ageLimit;
    private Boolean seatSelectable;
    private RegisterSeatLayoutDto seatLayout;

    protected UpdateEventRequest() {
    }
}
