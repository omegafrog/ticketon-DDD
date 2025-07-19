package org.codenbug.seat.infra;

import org.codenbug.message.SeatPurchasedCompleteEvent;
import org.codenbug.message.SeatPurchasedEvent;
import org.codenbug.seat.domain.Seat;
import org.codenbug.seat.domain.SeatLayout;
import org.codenbug.seat.domain.SeatLayoutRepository;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class SeatPurchasedEventConsumer {
	private final KafkaTemplate<String, Object> kafkaTemplate;
	private final SeatLayoutRepository seatLayoutRepository;

	public SeatPurchasedEventConsumer(SeatLayoutRepository seatLayoutRepository,
		KafkaTemplate<String, Object> kafkaTemplate, SeatLayoutRepository seatLayoutRepository1) {
		this.kafkaTemplate = kafkaTemplate;
		this.seatLayoutRepository = seatLayoutRepository1;
	}

	@KafkaListener(topics = "seat-purchased", groupId = "seat-purchased-seat-group")
	@Transactional
	public void consume(SeatPurchasedEvent event) {
		try {
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
			kafkaTemplate.send("seat-purchased-complete",
				new SeatPurchasedCompleteEvent(event.getUserId()));
		} catch (Exception e) {
			// TODO: 좌석이 변경되었을 시 보상 트랜잭션
		}

	}
}
