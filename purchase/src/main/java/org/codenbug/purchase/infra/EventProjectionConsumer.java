package org.codenbug.purchase.infra;

import org.codenbug.message.EventCreatedEvent;
import org.codenbug.message.EventUpdatedEvent;
import org.codenbug.purchase.query.model.EventProjection;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class EventProjectionConsumer {
    
    private final JpaPurchaseEventProjectionRepository eventProjectionRepository;
    
    public EventProjectionConsumer(JpaPurchaseEventProjectionRepository eventProjectionRepository) {
        this.eventProjectionRepository = eventProjectionRepository;
    }
    
    @KafkaListener(topics = EventCreatedEvent.TOPIC, groupId = "purchase-event-projection-group")
    @Transactional
    public void handleEventCreated(EventCreatedEvent event) {
        EventProjection projection = new EventProjection(
            event.getEventId(),
            event.getTitle(),
            event.getManagerId(),
            event.isSeatSelectable(),
            event.getSeatLayoutId()
        );
        eventProjectionRepository.save(projection);
    }
    
    @KafkaListener(topics = EventUpdatedEvent.TOPIC, groupId = "purchase-event-projection-group")
    @Transactional
    public void handleEventUpdated(EventUpdatedEvent event) {
        EventProjection existing = eventProjectionRepository.findById(event.getEventId()).orElse(null);
        if (existing != null) {
            // Update existing projection in-place
            existing.updateFrom(
                event.getTitle(),
                event.getManagerId(),
                event.isSeatSelectable(),
                event.getSeatLayoutId()
            );
            eventProjectionRepository.save(existing);
        }
    }
}