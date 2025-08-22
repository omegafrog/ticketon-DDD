package org.codenbug.message;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class EventDeletedEvent {
    public static final String TOPIC = "event-deleted";
    private String eventId;
    private String title;
    private String managerId;
    private Integer minPrice;
    private Integer maxPrice;
    private String location;
    private Long categoryId;
}