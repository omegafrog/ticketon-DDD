package org.codenbug.event.application.dto.response;

import java.io.Serializable;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.codenbug.event.domain.Event;
import org.codenbug.event.domain.EventInformation;
import org.codenbug.event.domain.EventStatus;

@NoArgsConstructor
@Getter
public class EventListResponse implements Serializable {

    String eventId;
    String category;
    EventInformationListDto information;
    LocalDateTime bookingStart;
    LocalDateTime bookingEnd;
    Integer viewCount;
    EventStatus status;
    Boolean seatSelectable;
    Boolean isDeleted;
    Integer minPrice;
    Integer maxPrice;

    public EventListResponse(String eventId, String category, EventInformation information,
        LocalDateTime bookingStart, LocalDateTime bookingEnd, Integer viewCount, EventStatus status,
        Boolean seatSelectable, Boolean isDeleted, Integer minPrice, Integer maxPrice) {
        this.eventId = eventId;
        this.category = category;
        this.information = new EventInformationListDto(information);
        this.bookingStart = bookingStart;
        this.bookingEnd = bookingEnd;
        this.viewCount = viewCount;
        this.status = status;
        this.seatSelectable = seatSelectable;
        this.isDeleted = isDeleted;
        this.minPrice = minPrice;
        this.maxPrice = maxPrice;
    }

    public EventListResponse(Event event, String categoryName) {
        this.eventId = event.getEventId().getEventId();
        this.category = categoryName;
        this.information = new EventInformationListDto(event.getEventInformation());
        this.bookingStart = event.getEventInformation().getBookingStart();
        this.bookingEnd = event.getEventInformation().getBookingEnd();
        this.viewCount = event.getEventInformation().getViewCount();
        this.status = event.getEventInformation().getStatus();
        this.seatSelectable = event.getEventInformation().getSeatSelectable();
        this.isDeleted = event.getMetaData().getDeleted();
        this.minPrice = event.getEventInformation().getMinPrice();
        this.maxPrice = event.getEventInformation().getMaxPrice();
    }

    public EventListResponse(Event event, String categoryName, Integer minPrice, Integer maxPrice) {
        this.eventId = event.getEventId().getEventId();
        this.category = categoryName;
        this.information = new EventInformationListDto(event.getEventInformation());
        this.bookingStart = event.getEventInformation().getBookingStart();
        this.bookingEnd = event.getEventInformation().getBookingEnd();
        this.viewCount = event.getEventInformation().getViewCount();
        this.status = event.getEventInformation().getStatus();
        this.seatSelectable = event.getEventInformation().getSeatSelectable();
        this.isDeleted = event.getMetaData().getDeleted();
        this.minPrice = minPrice;
        this.maxPrice = maxPrice;
    }


    @NoArgsConstructor
    @Getter
    public static class EventInformationListDto implements Serializable {

        private String title;
        private String thumbnailUrl;
        private Integer ageLimit;
        private String restrictions;
        private String description;
        private LocalDateTime bookingStart;
        private LocalDateTime bookingEnd;
        private LocalDateTime eventStart;
        private LocalDateTime eventEnd;
        private Integer viewCount;
        private Boolean seatSelectable;
        private EventStatus status;

        public EventInformationListDto(EventInformation information) {
            this.title = information.getTitle();
            this.thumbnailUrl = information.getThumbnailUrl();
            this.ageLimit = information.getAgeLimit();
            this.restrictions = information.getRestrictions();
            this.description = information.getDescription();
            this.bookingStart = information.getBookingStart();
            this.bookingEnd = information.getBookingEnd();
            this.viewCount = information.getViewCount();
            this.seatSelectable = information.getSeatSelectable();
            this.status = information.getStatus();
            this.eventStart = information.getEventStart();
            this.eventEnd = information.getEventEnd();
        }
    }
}