package org.codenbug.seat.infra;

import org.codenbug.message.SeatPurchasedEvent;
import org.codenbug.redislock.RedisLockService;
import org.codenbug.seat.domain.Seat;
import org.codenbug.seat.domain.SeatLayout;
import org.codenbug.seat.domain.SeatLayoutRepository;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class SeatPurchasedEventConsumer {
	private final SeatLayoutRepository seatLayoutRepository;
	private final RedisLockService redisLockService;
	private final ObjectMapper objectMapper;

	public SeatPurchasedEventConsumer(SeatLayoutRepository seatLayoutRepository1, RedisLockService redisLockService,
		ObjectMapper objectMapper) {
		this.seatLayoutRepository = seatLayoutRepository1;
		this.redisLockService = redisLockService;
		this.objectMapper = objectMapper;
	}

	@RabbitListener(queues = "seat-purchased")
	@Transactional
	public void consume(String message) {
		try {
			SeatPurchasedEvent event = objectMapper.readValue(message, SeatPurchasedEvent.class);
			SeatLayout seatLayout = seatLayoutRepository.findSeatLayout(event.getSeatLayoutId());
			for (String seatId : event.getSeatIds()){
				for(Seat seat : seatLayout.getSeats()){
					if(seat.getSeatId().getValue().equals(seatId)){
						seat.setAvailable(false);
					}else{
						throw new IllegalStateException("존재하지 않는 좌석입니다.");
					}
				}
			}
			redisLockService.releaseAllLocks(event.getUserId());
			redisLockService.releaseAllEntryQueueLocks(event.getUserId());
		} catch (Exception e) {
			// TODO: 좌석이 변경되었을 시 보상 트랜잭션
		}

	}
}
