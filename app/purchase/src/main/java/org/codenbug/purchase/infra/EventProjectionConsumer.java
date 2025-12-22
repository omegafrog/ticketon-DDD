package org.codenbug.purchase.infra;

import org.codenbug.message.EventCreatedEvent;
import org.codenbug.purchase.query.model.EventProjection;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class EventProjectionConsumer {

    private final JpaPurchaseEventProjectionRepository eventProjectionRepository;

    public EventProjectionConsumer(JpaPurchaseEventProjectionRepository eventProjectionRepository) {
        this.eventProjectionRepository = eventProjectionRepository;
    }

    //@KafkaListener(topics = EventCreatedEvent.TOPIC, groupId = "purchase-event-projection-group")
    @Transactional
    public void handleEventCreated(EventCreatedEvent event) {
        EventProjection projection = new EventProjection(event.getEventId(), event.getTitle(),
            event.getManagerId(), event.getSeatLayoutId(), event.isSeatSelectable(),
            event.getLocation(), event.getStartTime(), event.getEndTime(), 1L, // 초기 version
            "OPEN" // 초기 상태
        );
        eventProjectionRepository.save(projection);
    }
}
