package org.codenbug.purchase.infra;

import java.util.List;

import org.codenbug.message.SeatPurchasedCanceledEvent;
import org.codenbug.message.SeatPurchasedEvent;
import org.codenbug.purchase.domain.MessagePublisher;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class KafkaMessagePublisher implements MessagePublisher {
	private final KafkaTemplate<String, Object> kafkaTemplate;

	public KafkaMessagePublisher(KafkaTemplate<String, Object> kafkaTemplate) {
		this.kafkaTemplate = kafkaTemplate;
	}

	@Override
	public void publishSeatPurchasedEvent(String eventId, Long layoutId, List<String> seatIds, String userId) {
		kafkaTemplate.send("seat-purchased", new SeatPurchasedEvent(eventId, seatIds, layoutId, userId));
	}


	@Override
	public void publishSeatPurchaseCanceledEvent(List<String> seatIds, String purchaseId) {
		kafkaTemplate.send("seat-purchase-canceled", new SeatPurchasedCanceledEvent(seatIds, purchaseId));
	}
}

