package org.codenbug.message;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class EventCreatedEvent {
    public static final String TOPIC = "event-created";
    private String eventId;
    private String title;
    private String managerId;
    private Long seatLayoutId;
    private boolean seatSelectable;
    private String location;
    private String startTime;
    private String endTime;
}