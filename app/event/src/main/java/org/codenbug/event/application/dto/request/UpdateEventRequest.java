package org.codenbug.event.application.dto.request;

import java.time.LocalDateTime;
import lombok.Getter;
import org.codenbug.event.domain.EventStatus;
import org.codenbug.seat.global.RegisterSeatLayoutDto;

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
