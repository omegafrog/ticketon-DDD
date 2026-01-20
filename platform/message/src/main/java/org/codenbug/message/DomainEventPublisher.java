package org.codenbug.message;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class DomainEventPublisher {
    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;
    
    public DomainEventPublisher(RabbitTemplate rabbitTemplate, ObjectMapper objectMapper) {
        this.rabbitTemplate = rabbitTemplate;
        this.objectMapper = objectMapper;
    }
    
    @TransactionalEventListener
    public void handleEventCreated(EventCreatedEvent event) {
        sendAsJson(EventCreatedEvent.TOPIC, event);
    }
    
    @TransactionalEventListener
    public void handleEventUpdated(EventUpdatedEvent event) {
        sendAsJson(EventUpdatedEvent.TOPIC, event);
    }
    
    @TransactionalEventListener
    public void handleUserRegistered(UserRegisteredEvent event) {
        sendAsJson(UserRegisteredEvent.TOPIC, event);
    }
    
    @TransactionalEventListener
    public void handleSeatLayoutCreated(SeatLayoutCreatedEvent event) {
        sendAsJson(SeatLayoutCreatedEvent.TOPIC, event);
    }

    private void sendAsJson(String queue, Object payload) {
        try {
            String message = objectMapper.writeValueAsString(payload);
            rabbitTemplate.convertAndSend(queue, message);
        } catch (Exception e) {
            throw new IllegalStateException("메시지 직렬화에 실패했습니다.", e);
        }
    }
}
