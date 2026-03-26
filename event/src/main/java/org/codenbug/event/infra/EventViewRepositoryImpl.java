package org.codenbug.event.infra;

import java.util.List;

import org.codenbug.event.domain.QEvent;
import org.codenbug.event.domain.QSeatLayoutStats;
import org.codenbug.event.global.EventListFilter;
import org.codenbug.event.query.EventListProjection;
import org.codenbug.event.query.EventViewRepository;
import org.codenbug.event.query.RedisViewCountService;
import org.codenbug.seat.domain.QSeat;
import org.codenbug.seat.domain.QSeatLayout;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;

import jakarta.persistence.EntityNotFoundException;

@Repository
@Transactional(value = "readOnlyTransactionManager", readOnly = true)
public class EventViewRepositoryImpl implements EventViewRepository {

	private final JPAQueryFactory readOnlyQueryFactory;
	private final RedisViewCountService redisViewCountService;
	private final QEvent event = QEvent.event;
	private final QSeatLayout seatLayout = QSeatLayout.seatLayout;
	private final QSeatLayoutStats seatStats = QSeatLayoutStats.seatLayoutStats;

	public EventViewRepositoryImpl(@Qualifier("readOnlyQueryFactory") JPAQueryFactory readOnlyQueryFactory,
		RedisViewCountService redisViewCountService) {
		this.readOnlyQueryFactory = readOnlyQueryFactory;
		this.redisViewCountService = redisViewCountService;
	}

	@Override
	public Page<EventListProjection> findEventList(String keyword, EventListFilter filter, Pageable pageable) {
		// 동적 조건 생성
		BooleanBuilder whereClause = buildWhereClause(keyword, filter);

		// 메인 쿼리: DB 데이터 조회 (머터리얼라이즈 뷰 활용)
		JPAQuery<EventListProjection> query = readOnlyQueryFactory
			.select(Projections.constructor(EventListProjection.class,
				event.eventId.eventId,
				event.eventInformation.title,
				event.eventInformation.thumbnailUrl,
				event.eventInformation.eventStart,
				event.eventInformation.eventEnd,
				event.eventInformation.bookingStart,
				event.eventInformation.bookingEnd,
				seatStats.minPrice.coalesce(event.eventInformation.minPrice),
				seatStats.maxPrice.coalesce(event.eventInformation.maxPrice),
				event.eventInformation.viewCount, // DB viewCount
				event.eventInformation.status.stringValue(),
				event.eventInformation.categoryId.value,
				seatLayout.location.locationName,
				seatStats.seatCount.coalesce(0).longValue() // 🔥 최적화: 서브쿼리 → 머터리얼라이즈 뷰 조인
			))
			.from(event)
			.join(seatLayout).on(
				seatLayout.id.eq(event.seatLayoutId.value)
			)
			.leftJoin(seatStats).on(
				seatStats.layoutId.eq(event.seatLayoutId.value)
			)
			.where(whereClause)
			.offset(pageable.getOffset())
			.limit(pageable.getPageSize());

		// 정렬 조건 추가
		if (pageable.getSort().isSorted()) {
			pageable.getSort().forEach(order -> {
				if ("createdAt".equals(order.getProperty())) {
					if (order.isAscending()) {
						query.orderBy(event.metaData.createdAt.asc());
					} else {
						query.orderBy(event.metaData.createdAt.desc());
					}
				} else if ("eventStart".equals(order.getProperty())) {
					if (order.isAscending()) {
						query.orderBy(event.eventInformation.eventStart.asc());
					} else {
						query.orderBy(event.eventInformation.eventStart.desc());
					}
				}
			});
		} else {
			query.orderBy(event.metaData.createdAt.desc()); // 기본 정렬
		}

		List<EventListProjection> dbResults = query.fetch();

		// Redis에서 실시간 viewCount 조회 및 적용 (리스트용 - 캐싱하지 않음)
		List<EventListProjection> results = dbResults.stream()
			.map(dbResult -> {
				Integer redisViewCount = redisViewCountService.getViewCountForList(
					dbResult.getEventId(),
					dbResult.getDbViewCount()
				);
				dbResult.setRedisViewCount(redisViewCount);
				return dbResult;
			})
			.toList();

		// COUNT 쿼리
		Long total = readOnlyQueryFactory
			.select(event.count())
			.from(event)
			.where(whereClause)
			.fetchOne();

		return new PageImpl<>(results, pageable, total != null ? total : 0);
	}

	@Override
	public Page<EventListProjection> findManagerEventList(String managerId, Pageable pageable) {
		JPAQuery<EventListProjection> query = readOnlyQueryFactory
			.select(Projections.constructor(EventListProjection.class,
				event.eventId.eventId,
				event.eventInformation.title,
				event.eventInformation.thumbnailUrl,
				event.eventInformation.eventStart,
				event.eventInformation.eventEnd,
				event.eventInformation.bookingStart,
				event.eventInformation.bookingEnd,
				seatStats.minPrice.coalesce(event.eventInformation.minPrice),
				seatStats.maxPrice.coalesce(event.eventInformation.maxPrice),
				event.eventInformation.viewCount, // DB viewCount
				event.eventInformation.status.stringValue(),
				event.eventInformation.categoryId.value,
				seatLayout.location.locationName
			))
			.from(event)
			.join(seatLayout).on(seatLayout.id.eq(event.seatLayoutId.value)).fetchJoin()
			.leftJoin(seatStats).on(seatStats.layoutId.eq(event.seatLayoutId.value))
			.where(event.managerId.managerId.eq(managerId)
				.and(event.metaData.deleted.isFalse()))
			.offset(pageable.getOffset())
			.limit(pageable.getPageSize())
			.orderBy(event.metaData.createdAt.desc());

		List<EventListProjection> dbResults = query.fetch();

		// Redis에서 실시간 viewCount 조회 및 적용 (리스트용 - 캐싱하지 않음)
		List<EventListProjection> results = dbResults.stream()
			.map(dbResult -> {
				Integer redisViewCount = redisViewCountService.getViewCountForList(
					dbResult.getEventId(),
					dbResult.getDbViewCount()
				);
				dbResult.setRedisViewCount(redisViewCount);
				return dbResult;
			})
			.toList();

		Long total = readOnlyQueryFactory
			.select(event.count())
			.from(event)
			.where(event.managerId.managerId.eq(managerId)
				.and(event.metaData.deleted.isFalse()))
			.fetchOne();

		return new PageImpl<>(results, pageable, total != null ? total : 0);
	}

	@Override
	public Page<EventListProjection> findEventListWithCursor(String keyword, EventListFilter filter,
		String lastEventId, int size) {
		BooleanBuilder whereClause = buildWhereClause(keyword, filter);

		// 커서 조건 추가
		if (StringUtils.hasText(lastEventId)) {
			whereClause.and(event.eventId.eventId.gt(lastEventId));
		}

		List<EventListProjection> dbResults = readOnlyQueryFactory
			.select(Projections.constructor(EventListProjection.class,
				event.eventId.eventId,
				event.eventInformation.title,
				event.eventInformation.thumbnailUrl,
				event.eventInformation.eventStart,
				event.eventInformation.eventEnd,
				event.eventInformation.bookingStart,
				event.eventInformation.bookingEnd,
				seatStats.minPrice.coalesce(event.eventInformation.minPrice),
				seatStats.maxPrice.coalesce(event.eventInformation.maxPrice),
				event.eventInformation.viewCount, // DB viewCount
				event.eventInformation.status.stringValue(),
				event.eventInformation.categoryId.value,
				seatLayout.location.locationName
			))
			.from(event)
			.join(seatLayout).on(seatLayout.id.eq(event.seatLayoutId.value)).fetchJoin()
			.leftJoin(seatStats).on(seatStats.layoutId.eq(event.seatLayoutId.value))
			.where(whereClause)
			.orderBy(event.eventId.eventId.asc())
			.limit(size + 1) // 다음 페이지 존재 여부 확인용 +1
			.fetch();

		boolean hasNext = dbResults.size() > size;
		if (hasNext) {
			dbResults.remove(dbResults.size() - 1); // 마지막 요소 제거
		}

		// Redis에서 실시간 viewCount 조회 및 적용 (리스트용 - 캐싱하지 않음)
		List<EventListProjection> results = dbResults.stream()
			.map(dbResult -> {
				Integer redisViewCount = redisViewCountService.getViewCountForList(
					dbResult.getEventId(),
					dbResult.getDbViewCount()
				);
				dbResult.setRedisViewCount(redisViewCount);
				return dbResult;
			})
			.toList();

		return new PageImpl<>(results, Pageable.ofSize(size), results.size());
	}

	private BooleanBuilder buildWhereClause(String keyword, EventListFilter filter) {
		BooleanBuilder whereClause = new BooleanBuilder();

		// 기본 조건: 삭제되지 않은 이벤트
		whereClause.and(event.metaData.deleted.isFalse());

		// 키워드 검색
		if (StringUtils.hasText(keyword)) {
			whereClause.and(event.eventInformation.title.containsIgnoreCase(keyword));
		}

		// 필터 조건들 (filter가 null이 아닐 때만)
		if (filter != null) {
			// 위치 필터 (Location name)
			if (filter.getLocationList() != null && !filter.getLocationList().isEmpty()) {
				whereClause.and(seatLayout.location.locationName.in(filter.getLocationList()));
			}

			// 지역 필터 (RegionLocation enum) - 서브쿼리 접근법
			if (filter.getRegionLocationList() != null && !filter.getRegionLocationList().isEmpty()) {
				QSeatLayout subSeatLayout = new QSeatLayout("subSeatLayout");
				whereClause.and(event.seatLayoutId.value.in(
					readOnlyQueryFactory.select(subSeatLayout.id)
						.from(subSeatLayout)
						.where(subSeatLayout.regionLocation.in(filter.getRegionLocationList()))
				));
			}

			// 카테고리 필터 (리스트)
			if (filter.getEventCategoryList() != null && !filter.getEventCategoryList().isEmpty()) {
				whereClause.and(event.eventInformation.categoryId.value.in(filter.getEventCategoryList()));
			}
			
			// 카테고리 필터 (단일)
			if (filter.getCategoryId() != null) {
				whereClause.and(event.eventInformation.categoryId.value.eq(filter.getCategoryId()));
			}

			// 상태 필터
			if (filter.getEventStatusList() != null && !filter.getEventStatusList().isEmpty()) {
				whereClause.and(event.eventInformation.status.in(filter.getEventStatusList()));
			}

			// 날짜 범위 필터
			if (filter.getStartDate() != null && filter.getEndDate() != null) {
				whereClause.and(event.eventInformation.eventStart.between(
					filter.getStartDate(), filter.getEndDate()));
			}

			// 가격 범위 필터
			if (filter.getCostRange() != null) {
				if (filter.getCostRange().getMin() != null) {
					whereClause.and(event.eventInformation.minPrice.goe(filter.getCostRange().getMin()));
				}
				if (filter.getCostRange().getMax() != null) {
					whereClause.and(event.eventInformation.maxPrice.loe(filter.getCostRange().getMax()));
				}
			}
		}

		return whereClause;
	}

	@Override
	public EventListProjection findEventById(String eventId) {
		QSeat seat = QSeat.seat;
		EventListProjection dbResult = readOnlyQueryFactory
			.select(Projections.constructor(EventListProjection.class,
				event.eventId.eventId,
				event.eventInformation.title,
				event.eventInformation.thumbnailUrl,
				event.eventInformation.eventStart,
				event.eventInformation.eventEnd,
				event.eventInformation.bookingStart,
				event.eventInformation.bookingEnd,
				seatStats.minPrice.coalesce(event.eventInformation.minPrice),
				seatStats.maxPrice.coalesce(event.eventInformation.maxPrice),
				event.eventInformation.viewCount, // DB viewCount
				event.eventInformation.status.stringValue(),
				event.eventInformation.categoryId.value,
				seatLayout.location.locationName,
				seatStats.seatCount.longValue(),
				new CaseBuilder()
					.when(seat.available.isTrue())
					.then(1)
					.otherwise(0)
					.sum().longValue()
			))
			.from(event)
			.leftJoin(seatLayout).on(seatLayout.id.eq(event.seatLayoutId.value)).fetchJoin()
			.leftJoin(seatStats).on(seatStats.layoutId.eq(event.seatLayoutId.value))
			.leftJoin(seat).on(seatLayout.eq(seat.seatLayout)).fetchJoin()
			.where(event.eventId.eventId.eq(eventId)
				.and(event.metaData.deleted.isFalse()))
			.fetchOne();

		if(dbResult.getEventId() == null){
			throw new EntityNotFoundException("Event not found");
		}

		// Redis에서 실시간 viewCount 조회 및 적용
		Integer redisViewCount = redisViewCountService.getViewCount(
			dbResult.getEventId(),
			dbResult.getDbViewCount()
		);
		dbResult.setRedisViewCount(redisViewCount);

		return dbResult;
	}

	@Override
	@Async
	public void incrementViewCount(String eventId) {
		// Redis에서 조회수 증가 (DB 업데이트 대신)
		redisViewCountService.incrementViewCount(eventId);
	}
}
