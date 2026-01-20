package org.codenbug.purchase.infra;

import java.util.List;

import org.codenbug.message.SeatPurchasedCanceledEvent;
import org.codenbug.message.SeatPurchasedEvent;
import org.codenbug.purchase.domain.MessagePublisher;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class RabbitMessagePublisher implements MessagePublisher {
	private static final String SEAT_PURCHASED_QUEUE = "seat-purchased";
	private static final String SEAT_PURCHASE_CANCELED_QUEUE = "seat-purchase-canceled";

	private final RabbitTemplate rabbitTemplate;
	private final ObjectMapper objectMapper;

	public RabbitMessagePublisher(RabbitTemplate rabbitTemplate, ObjectMapper objectMapper) {
		this.rabbitTemplate = rabbitTemplate;
		this.objectMapper = objectMapper;
	}

	@Override
	public void publishSeatPurchasedEvent(String eventId, Long layoutId, List<String> seatIds, String userId) {
		SeatPurchasedEvent event = new SeatPurchasedEvent(eventId, seatIds, layoutId, userId);
		sendAsJson(SEAT_PURCHASED_QUEUE, event);
	}

	@Override
	public void publishSeatPurchaseCanceledEvent(List<String> seatIds, String purchaseId) {
		SeatPurchasedCanceledEvent event = new SeatPurchasedCanceledEvent(seatIds, purchaseId);
		sendAsJson(SEAT_PURCHASE_CANCELED_QUEUE, event);
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

