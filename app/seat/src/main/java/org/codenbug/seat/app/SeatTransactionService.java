package org.codenbug.seat.app;

import java.time.Duration;
import java.util.UUID;

import org.codenbug.redislock.RedisLockService;
import org.codenbug.seat.domain.Seat;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class SeatTransactionService {

	private final RedisLockService redisLockService;
	static final String SEAT_LOCK_KEY_PREFIX = "seat:lock:";

	@Transactional
	public void reserveSeat(Seat seat, String userId, String eventId, String seatId) {
		String lockKey = SEAT_LOCK_KEY_PREFIX + userId + ":" + eventId + ":" + seatId;
		String lockValue = UUID.randomUUID().toString();
		log.info("lock key: {}, lock value = {}", lockKey, lockValue);

		boolean lockSuccess = redisLockService.tryLock(lockKey, lockValue, Duration.ofMinutes(5));
		log.info("lock success: {}", lockSuccess);
		if (!lockSuccess) {
			throw new IllegalStateException("[reserveSeat] 이미 선택된 좌석이 있습니다.");
		}

		try {
			seat.reserve();
			log.info("[reserveSeat] seat id {}가 예매되었습니다. 예매 상태: {}", seat.getSeatId().getValue(), seat.isAvailable());
		} catch (Exception e) {
			// 예외 발생 시 즉시 락 해제
			redisLockService.unlock(lockKey, lockValue);
			seat.cancelReserve();
			throw e;
		}

	}
}