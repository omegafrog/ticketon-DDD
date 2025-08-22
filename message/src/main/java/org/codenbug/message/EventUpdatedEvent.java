package org.codenbug.message;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class EventUpdatedEvent {
    public static final String TOPIC = "event-updated";
    private String eventId;
    private String title;
    private String managerId;
    private Long seatLayoutId;
    private boolean seatSelectable;
    private Integer seatCount;
    private String location;
    private String startTime;
    private String endTime;
    private Integer minPrice;
    private Integer maxPrice;
    private Long categoryId;
}