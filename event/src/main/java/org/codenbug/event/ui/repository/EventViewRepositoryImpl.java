package org.codenbug.event.ui.repository;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.codenbug.event.domain.QEvent;
import org.codenbug.event.domain.QSeatLayoutId;
import org.codenbug.event.ui.projection.EventListProjectionWithRedis;
import org.codenbug.event.global.EventListFilter;
import org.codenbug.event.ui.service.RedisViewCountService;
import org.codenbug.seat.domain.QSeatLayout;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Repository
public class EventViewRepositoryImpl implements EventViewRepository {
    
    private final JPAQueryFactory queryFactory;
    private final RedisViewCountService redisViewCountService;
    private final QEvent event = QEvent.event;
    private final QSeatLayout seatLayout = QSeatLayout.seatLayout;
    public EventViewRepositoryImpl(JPAQueryFactory queryFactory, RedisViewCountService redisViewCountService) {
        this.queryFactory = queryFactory;
        this.redisViewCountService = redisViewCountService;
    }
    
    @Override
    public Page<EventListProjectionWithRedis> findEventList(String keyword, EventListFilter filter, Pageable pageable) {
        // 동적 조건 생성
        BooleanBuilder whereClause = buildWhereClause(keyword, filter);
        
        // 메인 쿼리: DB 데이터 조회 (viewCount 포함)
        JPAQuery<EventListProjectionWithRedis> query = queryFactory
            .select(Projections.constructor(EventListProjectionWithRedis.class,
                event.eventId.eventId,
                event.eventInformation.title,
                event.eventInformation.thumbnailUrl,
                event.eventInformation.eventStart,
                event.eventInformation.eventEnd,
                event.eventInformation.bookingStart,
                event.eventInformation.bookingEnd,
                event.eventInformation.minPrice,
                event.eventInformation.maxPrice,
                event.eventInformation.viewCount, // DB viewCount
                event.eventInformation.status.stringValue(),
                event.eventInformation.categoryId.value,
                seatLayout.location.locationName
            ))
            .from(event)
            .join(seatLayout).on(
                seatLayout.id.eq(event.seatLayoutId.value)
            ).fetchJoin()
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
        
        List<EventListProjectionWithRedis> dbResults = query.fetch();
        
        // Redis에서 실시간 viewCount 조회 및 적용
        List<EventListProjectionWithRedis> results = dbResults.stream()
            .map(dbResult -> {
                Integer redisViewCount = redisViewCountService.getViewCount(
                    dbResult.getEventId(), 
                    dbResult.getDbViewCount()
                );
                dbResult.setRedisViewCount(redisViewCount);
                return dbResult;
            })
            .toList();
        
        // COUNT 쿼리
        Long total = queryFactory
            .select(event.count())
            .from(event)
            .where(whereClause)
            .fetchOne();
        
        return new PageImpl<>(results, pageable, total != null ? total : 0);
    }
    
    @Override
    public Page<EventListProjectionWithRedis> findManagerEventList(String managerId, Pageable pageable) {
        JPAQuery<EventListProjectionWithRedis> query = queryFactory
            .select(Projections.constructor(EventListProjectionWithRedis.class,
                event.eventId.eventId,
                event.eventInformation.title,
                event.eventInformation.thumbnailUrl,
                event.eventInformation.eventStart,
                event.eventInformation.eventEnd,
                event.eventInformation.bookingStart,
                event.eventInformation.bookingEnd,
                event.eventInformation.minPrice,
                event.eventInformation.maxPrice,
                event.eventInformation.viewCount, // DB viewCount
                event.eventInformation.status.stringValue(),
                event.eventInformation.categoryId.value,
                seatLayout.location.locationName
            ))
            .from(event)
            .join(seatLayout).on(seatLayout.id.eq(event.seatLayoutId.value)).fetchJoin()
            .where(event.managerId.managerId.eq(managerId)
                .and(event.metaData.deleted.isFalse()))
            .offset(pageable.getOffset())
            .limit(pageable.getPageSize())
            .orderBy(event.metaData.createdAt.desc());
        
        List<EventListProjectionWithRedis> dbResults = query.fetch();
        
        // Redis에서 실시간 viewCount 조회 및 적용
        List<EventListProjectionWithRedis> results = dbResults.stream()
            .map(dbResult -> {
                Integer redisViewCount = redisViewCountService.getViewCount(
                    dbResult.getEventId(), 
                    dbResult.getDbViewCount()
                );
                dbResult.setRedisViewCount(redisViewCount);
                return dbResult;
            })
            .toList();
        
        Long total = queryFactory
            .select(event.count())
            .from(event)
            .where(event.managerId.managerId.eq(managerId)
                .and(event.metaData.deleted.isFalse()))
            .fetchOne();
        
        return new PageImpl<>(results, pageable, total != null ? total : 0);
    }
    
    @Override
    public Page<EventListProjectionWithRedis> findEventListWithCursor(String keyword, EventListFilter filter, 
                                                            String lastEventId, int size) {
        BooleanBuilder whereClause = buildWhereClause(keyword, filter);
        
        // 커서 조건 추가
        if (StringUtils.hasText(lastEventId)) {
            whereClause.and(event.eventId.eventId.gt(lastEventId));
        }
        
        List<EventListProjectionWithRedis> dbResults = queryFactory
            .select(Projections.constructor(EventListProjectionWithRedis.class,
                event.eventId.eventId,
                event.eventInformation.title,
                event.eventInformation.thumbnailUrl,
                event.eventInformation.eventStart,
                event.eventInformation.eventEnd,
                event.eventInformation.bookingStart,
                event.eventInformation.bookingEnd,
                event.eventInformation.minPrice,
                event.eventInformation.maxPrice,
                event.eventInformation.viewCount, // DB viewCount
                event.eventInformation.status.stringValue(),
                event.eventInformation.categoryId.value,
                seatLayout.location.locationName
            ))
            .from(event)
            .join(seatLayout).on(seatLayout.id.eq(event.seatLayoutId.value)).fetchJoin()
            .where(whereClause)
            .orderBy(event.eventId.eventId.asc())
            .limit(size + 1) // 다음 페이지 존재 여부 확인용 +1
            .fetch();
        
        boolean hasNext = dbResults.size() > size;
        if (hasNext) {
            dbResults.remove(dbResults.size() - 1); // 마지막 요소 제거
        }
        
        // Redis에서 실시간 viewCount 조회 및 적용
        List<EventListProjectionWithRedis> results = dbResults.stream()
            .map(dbResult -> {
                Integer redisViewCount = redisViewCountService.getViewCount(
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
            // 위치 필터
            if (filter.getLocationList() != null && !filter.getLocationList().isEmpty()) {
                whereClause.and(seatLayout.location.locationName.in(filter.getLocationList()));
            }

            // 카테고리 필터
            if (filter.getEventCategoryList() != null && !filter.getEventCategoryList().isEmpty()) {
                whereClause.and(event.eventInformation.categoryId.value.in(filter.getEventCategoryList()));
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
    public EventListProjectionWithRedis findEventById(String eventId) {
        EventListProjectionWithRedis dbResult = queryFactory
            .select(Projections.constructor(EventListProjectionWithRedis.class,
                event.eventId.eventId,
                event.eventInformation.title,
                event.eventInformation.thumbnailUrl,
                event.eventInformation.eventStart,
                event.eventInformation.eventEnd,
                event.eventInformation.bookingStart,
                event.eventInformation.bookingEnd,
                event.eventInformation.minPrice,
                event.eventInformation.maxPrice,
                event.eventInformation.viewCount, // DB viewCount
                event.eventInformation.status.stringValue(),
                event.eventInformation.categoryId.value,
                seatLayout.location.locationName
            ))
            .from(event)
            .join(seatLayout).on(seatLayout.id.eq(event.seatLayoutId.value)).fetchJoin()
            .where(event.eventId.eventId.eq(eventId)
                .and(event.metaData.deleted.isFalse()))
            .fetchOne();
        
        if (dbResult == null) {
            return null;
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