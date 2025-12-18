package org.codenbug.event.batch.reader;

import com.querydsl.core.group.GroupBy;
import com.querydsl.jpa.impl.JPAQueryFactory;

import org.codenbug.event.batch.dto.ViewCountSyncDto;
import org.codenbug.event.domain.QEvent;
import org.codenbug.event.query.RedisViewCountService;
import org.springframework.batch.item.ItemReader;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * ViewCount 동기화를 위한 ItemReader
 * Redis와 DB의 viewCount를 비교하여 동기화 대상 이벤트들을 읽어옴
 */
@Slf4j
@Component
public class ViewCountSyncReader implements ItemReader<ViewCountSyncDto> {

	private final JPAQueryFactory queryFactory;
	private final RedisViewCountService redisViewCountService;
	private final QEvent event = QEvent.event;

	private Iterator<ViewCountSyncDto> syncDataIterator;
	private boolean initialized = false;

	public ViewCountSyncReader(@Qualifier("readOnlyQueryFactory") JPAQueryFactory queryFactory,
		RedisViewCountService redisViewCountService) {
		this.queryFactory = queryFactory;
		this.redisViewCountService = redisViewCountService;
	}

	@Override
	public ViewCountSyncDto read() {
		if (!initialized) {
			initialize();
		}

		if (syncDataIterator != null && syncDataIterator.hasNext()) {
			ViewCountSyncDto syncDto = syncDataIterator.next();
			log.debug("Reading sync data: {}", syncDto);
			return syncDto;
		}

		return null; // 읽을 데이터가 없으면 null 반환 (배치 종료)
	}

	private void initialize() {
		log.info("Initializing ViewCount sync reader...");

		try {
			// Redis에서 모든 viewCount 데이터 조회
			Map<String, Integer> redisViewCounts = redisViewCountService.createViewCountSnapshot();

			if (redisViewCounts.isEmpty()) {
				log.info("No viewCount data found in Redis");
				syncDataIterator = List.<ViewCountSyncDto>of().iterator();
				initialized = true;
				return;
			}

			// DB에서 해당 이벤트들의 현재 viewCount 조회
			Map<String, Integer> dbViewCounts = queryFactory
				.select(event.eventId.eventId, event.eventInformation.viewCount)
				.from(event)
				.where(event.eventId.eventId.in(redisViewCounts.keySet())
					.and(event.metaData.deleted.isFalse()))
				.transform(GroupBy.groupBy(event.eventId.eventId).as(event.eventInformation.viewCount));

			// 동기화 대상 데이터 생성
			List<ViewCountSyncDto> syncData = redisViewCounts.entrySet().stream()
				.map(entry -> {
					String eventId = entry.getKey();
					Integer redisCount = entry.getValue();
					Integer dbCount = dbViewCounts.getOrDefault(eventId, 0);

					return new ViewCountSyncDto(eventId, redisCount, dbCount);
				})
				.filter(ViewCountSyncDto::needsSync) // 동기화가 필요한 것만 필터링
				.toList();

			syncDataIterator = syncData.iterator();

			log.info("Initialized ViewCount sync reader with {} sync targets out of {} Redis entries",
				syncData.size(), redisViewCounts.size());

		} catch (Exception e) {
			log.error("Failed to initialize ViewCount sync reader", e);
			syncDataIterator = List.<ViewCountSyncDto>of().iterator();
		}

		initialized = true;
	}

	/**
	 * 배치 재시작 시 초기화 상태 리셋
	 */
	public void reset() {
		initialized = false;
		syncDataIterator = null;
	}
}