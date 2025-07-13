package org.codenbug.messagedispatcher.thread;

import java.util.concurrent.atomic.AtomicLong;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class PromotionCounterThread {

	private final AtomicLong promoteCounter;
	/**
	 * 1초마다 실행되어 초당 승격 처리량(TPS)을 로깅합니다.
	 */
	@Scheduled(fixedRate = 1000)
	public void logPromotionThroughput() {
		// getAndSet: 현재 값을 가져온 후 0으로 리셋하는 원자적(atomic) 연산입니다.
		long count = promoteCounter.getAndSet(0);

		// 승격된 사용자가 있을 경우에만 로그를 남깁니다.
		if (count > 0) {
			log.info("[METRICS] Promotions per second (TPS): {}", count);
		}
	}
}
