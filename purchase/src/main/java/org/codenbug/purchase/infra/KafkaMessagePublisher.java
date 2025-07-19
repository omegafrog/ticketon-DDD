package org.codenbug.purchase.infra;

import java.util.List;

import org.codenbug.message.SeatPurchasedCanceledEvent;
import org.codenbug.message.SeatPurchasedCompleteEvent;
import org.codenbug.message.SeatPurchasedEvent;
import org.codenbug.purchase.app.RedisLockService;
import org.codenbug.purchase.domain.MessagePublisher;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class KafkaMessagePublisher implements MessagePublisher {
	private final KafkaTemplate<String, Object> kafkaTemplate;
	private final RedisLockService redisLockService;

	public KafkaMessagePublisher(KafkaTemplate<String, Object> kafkaTemplate, RedisLockService redisLockService) {
		this.kafkaTemplate = kafkaTemplate;
		this.redisLockService = redisLockService;
	}

	@Override
	public void publishSeatPurchasedEvent(String eventId, Long layoutId, List<String> seatIds, String userId) {
		kafkaTemplate.send("seat-purchased", new SeatPurchasedEvent(eventId, seatIds, layoutId, userId));
	}

	@KafkaListener(topics = "seat-purchased-complete", groupId = "seat-purchased-success-group")
	public void consume(SeatPurchasedCompleteEvent event){
		redisLockService.releaseAllLocks(event.getUserId());
		redisLockService.releaseAllEntryQueueLocks(event.getUserId());
	}

	@Override
	public void publishSeatPurchaseCanceledEvent(List<String> seatIds, String purchaseId) {
		kafkaTemplate.send("seat-purchase-canceled", new SeatPurchasedCanceledEvent(seatIds, purchaseId));
	}
}

