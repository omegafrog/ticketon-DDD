package org.codenbug.seat.infra;

import lombok.extern.slf4j.Slf4j;
import org.codenbug.message.SeatPurchasedEvent;
import org.codenbug.redislock.RedisLockService;
import org.codenbug.seat.domain.Seat;
import org.codenbug.seat.domain.SeatLayout;
import org.codenbug.seat.domain.SeatLayoutRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
public class SeatPurchasedEventConsumer {

    private final SeatLayoutRepository seatLayoutRepository;
    private final RedisLockService redisLockService;

    public SeatPurchasedEventConsumer(SeatLayoutRepository seatLayoutRepository1,
        RedisLockService redisLockService) {
        this.seatLayoutRepository = seatLayoutRepository1;
        this.redisLockService = redisLockService;
    }

    //@KafkaListener(topics = "seat-purchased", groupId = "seat-purchased-seat-group")
    @Transactional
    public void consume(SeatPurchasedEvent event) {
        try {
            SeatLayout seatLayout = seatLayoutRepository.findSeatLayout(event.getSeatLayoutId());
            for (String seatId : event.getSeatIds()) {
                for (Seat seat : seatLayout.getSeats()) {
                    if (seat.getSeatId().getValue().equals(seatId)) {
                        seat.setAvailable(false);
                    } else {
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
