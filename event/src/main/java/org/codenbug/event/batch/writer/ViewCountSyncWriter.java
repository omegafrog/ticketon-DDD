package org.codenbug.event.batch.writer;

import com.querydsl.jpa.impl.JPAQueryFactory;

import org.codenbug.event.batch.dto.ViewCountSyncDto;
import org.codenbug.event.domain.QEvent;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Comparator;
import java.util.List;

/**
 * ViewCount 동기화를 위한 ItemWriter
 * 청크 단위로 bulk update를 수행하여 성능 최적화
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ViewCountSyncWriter implements ItemWriter<ViewCountSyncDto> {
	@PersistenceContext
	private final EntityManager entityManager;

	@Override
	@Transactional
	public void write(Chunk<? extends ViewCountSyncDto> chunk) {
		List<? extends ViewCountSyncDto> items = chunk.getItems();

		if (items.isEmpty()) {
			return;
		}
		items.sort(Comparator.comparing((ViewCountSyncDto o) -> o.getEventId()));

		log.info("Writing {} viewCount sync items", items.size());

		int totalUpdated = 0;
		StringBuilder builder = new StringBuilder();
		builder.append("INSERT INTO event (id, view_count) values");
		// 각 아이템에 대해 개별 업데이트 수행
		for (int i = 0; i < items.size(); i++) {
			if (i > 0)
				builder.append(", ");
			builder.append("( ?, ? )");
		}
		builder.append(" ON DUPLICATE KEY UPDATE id = VALUES(id);");
		Query query = entityManager.createNativeQuery(builder.toString());

		int idx = 1;
		for (ViewCountSyncDto syncDto : items) {
			query.setParameter(idx++, syncDto.getEventId());
			query.setParameter(idx++, syncDto.getRedisViewCount());
		}
		query.executeUpdate();

		// 업데이트 통계 로깅
		if (log.isInfoEnabled()) {
			int totalIncrement = items.stream()
				.mapToInt(ViewCountSyncDto::getIncrementAmount)
				.sum();
			log.info("Total viewCount increment in this chunk: {}", totalIncrement);
		}
	}
}