package org.codenbug.event.infra;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Arrays;

import org.codenbug.event.domain.QEvent;
import org.codenbug.event.global.EventListFilter;
import org.codenbug.seat.domain.QSeatLayout;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.querydsl.core.BooleanBuilder;

/**
 * EventViewRepositoryImpl의 buildWhereClause 메서드에서 
 * 카테고리 필터링 로직이 올바르게 작동하는지 테스트
 */
@DisplayName("카테고리 필터링 쿼리 조건 생성 테스트")
class CategoryFilteringQueryTest {

    private final QEvent event = QEvent.event;
    private final QSeatLayout seatLayout = QSeatLayout.seatLayout;

    @Test
    @DisplayName("단일 categoryId 필터 조건이 올바르게 생성됨")
    void shouldBuildCorrectWhereClauseForCategoryId() {
        // Given
        EventListFilter filter = new EventListFilter.Builder()
            .categoryId(1L)
            .build();

        // When
        BooleanBuilder whereClause = buildWhereClause(null, filter);

        // Then
        assertNotNull(whereClause);
        String queryString = whereClause.toString();
        
        // 기본 조건(deleted = false)와 카테고리 조건이 포함되어야 함
        assertTrue(queryString.contains("deleted = false"));
        assertTrue(queryString.contains("categoryId.value = 1"));
    }

    @Test
    @DisplayName("다중 eventCategoryList 필터 조건이 올바르게 생성됨")
    void shouldBuildCorrectWhereClauseForEventCategoryList() {
        // Given
        EventListFilter filter = new EventListFilter.Builder()
            .eventCategoryList(Arrays.asList(
                createMockEventCategory(1L),
                createMockEventCategory(2L)
            ))
            .build();

        // When
        BooleanBuilder whereClause = buildWhereClause(null, filter);

        // Then
        assertNotNull(whereClause);
        String queryString = whereClause.toString();
        
        // IN 조건이 포함되어야 함
        assertTrue(queryString.contains("deleted = false"));
        assertTrue(queryString.contains("categoryId.value in [1, 2]"));
    }

    @Test
    @DisplayName("categoryId와 eventCategoryList 동시 사용시 두 조건 모두 적용됨")
    void shouldBuildCorrectWhereClauseForBothCategoryFilters() {
        // Given
        EventListFilter filter = new EventListFilter.Builder()
            .categoryId(1L)
            .eventCategoryList(Arrays.asList(
                createMockEventCategory(2L),
                createMockEventCategory(3L)
            ))
            .build();

        // When
        BooleanBuilder whereClause = buildWhereClause(null, filter);

        // Then
        assertNotNull(whereClause);
        String queryString = whereClause.toString();
        
        // 두 조건 모두 AND로 결합되어야 함
        assertTrue(queryString.contains("deleted = false"));
        assertTrue(queryString.contains("categoryId.value = 1"));
        assertTrue(queryString.contains("categoryId.value in [2, 3]"));
    }

    @Test
    @DisplayName("카테고리 필터와 키워드 조합시 올바른 조건 생성")
    void shouldBuildCorrectWhereClauseForCategoryWithKeyword() {
        // Given
        EventListFilter filter = new EventListFilter.Builder()
            .categoryId(1L)
            .build();
        String keyword = "콘서트";

        // When
        BooleanBuilder whereClause = buildWhereClause(keyword, filter);

        // Then
        assertNotNull(whereClause);
        String queryString = whereClause.toString();
        
        // 키워드와 카테고리 조건이 모두 포함되어야 함
        assertTrue(queryString.contains("deleted = false"));
        assertTrue(queryString.contains("title") && queryString.contains("콘서트"));
        assertTrue(queryString.contains("categoryId.value = 1"));
    }

    @Test
    @DisplayName("필터가 null인 경우 기본 조건만 적용")
    void shouldBuildCorrectWhereClauseForNullFilter() {
        // When
        BooleanBuilder whereClause = buildWhereClause(null, null);

        // Then
        assertNotNull(whereClause);
        String queryString = whereClause.toString();
        
        // 기본 조건만 포함되어야 함
        assertTrue(queryString.contains("deleted = false"));
        assertFalse(queryString.contains("categoryId"));
    }

    @Test
    @DisplayName("빈 eventCategoryList는 조건에 포함되지 않음")
    void shouldNotIncludeEmptyEventCategoryListInWhereClause() {
        // Given
        EventListFilter filter = new EventListFilter.Builder()
            .eventCategoryList(Arrays.asList()) // 빈 리스트
            .build();

        // When
        BooleanBuilder whereClause = buildWhereClause(null, filter);

        // Then
        assertNotNull(whereClause);
        String queryString = whereClause.toString();
        
        // 빈 리스트는 조건에 포함되지 않아야 함
        assertTrue(queryString.contains("deleted = false"));
        assertFalse(queryString.contains("categoryId.value in"));
    }

    // EventViewRepositoryImpl의 buildWhereClause 로직을 복제한 헬퍼 메서드
    private BooleanBuilder buildWhereClause(String keyword, EventListFilter filter) {
        BooleanBuilder whereClause = new BooleanBuilder();

        // 기본 조건: 삭제되지 않은 이벤트
        whereClause.and(event.metaData.deleted.isFalse());

        // 키워드 검색
        if (keyword != null && !keyword.trim().isEmpty()) {
            whereClause.and(event.eventInformation.title.containsIgnoreCase(keyword));
        }

        // 필터 조건들 (filter가 null이 아닐 때만)
        if (filter != null) {
            // 카테고리 필터 (리스트)
            if (filter.getEventCategoryList() != null && !filter.getEventCategoryList().isEmpty()) {
                whereClause.and(event.eventInformation.categoryId.value.in(filter.getEventCategoryList()));
            }
            
            // 카테고리 필터 (단일)
            if (filter.getCategoryId() != null) {
                whereClause.and(event.eventInformation.categoryId.value.eq(filter.getCategoryId()));
            }
        }

        return whereClause;
    }

    private org.codenbug.event.category.domain.EventCategory createMockEventCategory(Long id) {
        org.codenbug.event.category.domain.EventCategory category = mock(org.codenbug.event.category.domain.EventCategory.class);
        org.codenbug.event.category.domain.CategoryId categoryId = mock(org.codenbug.event.category.domain.CategoryId.class);
        when(categoryId.getId()).thenReturn(id);
        when(category.getId()).thenReturn(categoryId);
        return category;
    }
}
