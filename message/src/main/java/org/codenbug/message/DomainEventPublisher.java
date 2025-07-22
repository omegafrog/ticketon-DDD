package org.codenbug.message;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class DomainEventPublisher {
    private final KafkaTemplate<String, Object> kafkaTemplate;
    
    public DomainEventPublisher(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }
    
    @TransactionalEventListener
    public void handleEventCreated(EventCreatedEvent event) {
        kafkaTemplate.send(EventCreatedEvent.TOPIC, event.getEventId(), event);
    }
    
    @TransactionalEventListener
    public void handleEventUpdated(EventUpdatedEvent event) {
        kafkaTemplate.send(EventUpdatedEvent.TOPIC, event.getEventId(), event);
    }
    
    @TransactionalEventListener
    public void handleUserRegistered(UserRegisteredEvent event) {
        kafkaTemplate.send(UserRegisteredEvent.TOPIC, event.getUserId(), event);
    }
}