package org.codenbug.event.global;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import org.codenbug.categoryid.domain.CategoryId;
import org.codenbug.categoryid.domain.EventCategory;
import org.codenbug.event.application.dto.CostRange;
import org.codenbug.event.application.dto.EventListFilter;
import org.codenbug.event.domain.EventStatus;
import org.codenbug.seat.domain.Location;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("EventListFilter 단위 테스트")
class EventListFilterTest {

    @Nested
    @DisplayName("Builder 패턴 테스트")
    class BuilderTest {

        @Test
        @DisplayName("categoryId 단일 카테고리 필터 설정")
        void shouldSetCategoryIdInFilter() {
            // Given
            Long expectedCategoryId = 1L;

            // When
            EventListFilter filter = new EventListFilter.Builder()
                .categoryId(expectedCategoryId)
                .build();

            // Then
            assertEquals(expectedCategoryId, filter.getCategoryId());
        }

        @Test
        @DisplayName("eventCategoryList 다중 카테고리 필터 설정")
        void shouldSetEventCategoryListInFilter() {
            // Given
            List<EventCategory> categories = Arrays.asList(
                createEventCategory(1L),
                createEventCategory(2L)
            );
            List<Long> expectedCategoryIds = Arrays.asList(1L, 2L);

            // When
            EventListFilter filter = new EventListFilter.Builder()
                .eventCategoryList(categories)
                .build();

            // Then
            assertEquals(expectedCategoryIds, filter.getEventCategoryList());
        }

        @Test
        @DisplayName("categoryId와 eventCategoryList 동시 설정")
        void shouldSetBothCategoryIdAndEventCategoryList() {
            // Given
            Long categoryId = 3L;
            List<EventCategory> categories = Arrays.asList(
                createEventCategory(1L),
                createEventCategory(2L)
            );

            // When
            EventListFilter filter = new EventListFilter.Builder()
                .categoryId(categoryId)
                .eventCategoryList(categories)
                .build();

            // Then
            assertEquals(categoryId, filter.getCategoryId());
            assertEquals(Arrays.asList(1L, 2L), filter.getEventCategoryList());
        }

        @Test
        @DisplayName("다른 필터들과 함께 categoryId 설정")
        void shouldSetCategoryIdWithOtherFilters() {
            // Given
            Long categoryId = 1L;
            CostRange costRange = new CostRange(1000, 5000);
            List<Location> locations = Arrays.asList(createLocation("서울"));
            List<EventStatus> statuses = Arrays.asList(EventStatus.OPEN);
            LocalDateTime startDate = LocalDateTime.of(2024, 1, 1, 0, 0);
            LocalDateTime endDate = LocalDateTime.of(2024, 12, 31, 23, 59);

            // When
            EventListFilter filter = new EventListFilter.Builder()
                .categoryId(categoryId)
                .costRange(costRange)
                .locationList(locations)
                .eventStatusList(statuses)
                .startDate(startDate)
                .endDate(endDate)
                .build();

            // Then
            assertEquals(categoryId, filter.getCategoryId());
            assertEquals(costRange, filter.getCostRange());
            assertEquals(Arrays.asList("서울"), filter.getLocationList());
            assertEquals(statuses, filter.getEventStatusList());
            assertEquals(startDate, filter.getStartDate());
            assertEquals(endDate, filter.getEndDate());
        }

        @Test
        @DisplayName("null 카테고리 설정")
        void shouldHandleNullCategory() {
            // When
            EventListFilter filter = new EventListFilter.Builder()
                .categoryId(null)
                .eventCategoryList(null)
                .build();

            // Then
            assertNull(filter.getCategoryId());
            assertNull(filter.getEventCategoryList());
        }
    }

    @Nested
    @DisplayName("canFiltered() 메소드 테스트")
    class CanFilteredTest {

        @Test
        @DisplayName("categoryId만 설정된 경우 true 반환")
        void shouldReturnTrueWhenOnlyCategoryIdIsSet() {
            // Given
            EventListFilter filter = new EventListFilter.Builder()
                .categoryId(1L)
                .build();

            // When & Then
            assertTrue(filter.canFiltered());
        }

        @Test
        @DisplayName("eventCategoryList만 설정된 경우 true 반환")
        void shouldReturnTrueWhenOnlyEventCategoryListIsSet() {
            // Given
            List<EventCategory> categories = Arrays.asList(createEventCategory(1L));
            EventListFilter filter = new EventListFilter.Builder()
                .eventCategoryList(categories)
                .build();

            // When & Then
            assertTrue(filter.canFiltered());
        }

        @Test
        @DisplayName("categoryId와 eventCategoryList 모두 설정된 경우 true 반환")
        void shouldReturnTrueWhenBothCategoryFiltersAreSet() {
            // Given
            List<EventCategory> categories = Arrays.asList(createEventCategory(2L));
            EventListFilter filter = new EventListFilter.Builder()
                .categoryId(1L)
                .eventCategoryList(categories)
                .build();

            // When & Then
            assertTrue(filter.canFiltered());
        }

        @Test
        @DisplayName("빈 eventCategoryList는 필터링 불가로 판단")
        void shouldReturnFalseWhenEventCategoryListIsEmpty() {
            // Given
            EventListFilter filter = new EventListFilter.Builder()
                .eventCategoryList(Arrays.asList())
                .build();

            // When & Then
            assertFalse(filter.canFiltered());
        }

        @Test
        @DisplayName("다른 필터들과 함께 categoryId가 설정된 경우 true 반환")
        void shouldReturnTrueWhenCategoryIdIsSetWithOtherFilters() {
            // Given
            EventListFilter filter = new EventListFilter.Builder()
                .categoryId(1L)
                .costRange(new CostRange(1000, 5000))
                .build();

            // When & Then
            assertTrue(filter.canFiltered());
        }

        @Test
        @DisplayName("아무 필터도 설정되지 않은 경우 false 반환")
        void shouldReturnFalseWhenNoFiltersAreSet() {
            // Given
            EventListFilter filter = new EventListFilter.Builder()
                .build();

            // When & Then
            assertFalse(filter.canFiltered());
        }

        @Test
        @DisplayName("null 카테고리 필터들만 설정된 경우 false 반환")
        void shouldReturnFalseWhenOnlyNullCategoryFiltersAreSet() {
            // Given
            EventListFilter filter = new EventListFilter.Builder()
                .categoryId(null)
                .eventCategoryList(null)
                .build();

            // When & Then
            assertFalse(filter.canFiltered());
        }
    }

    @Nested
    @DisplayName("Edge Case 테스트")
    class EdgeCaseTest {

        @Test
        @DisplayName("categoryId가 0인 경우 처리")
        void shouldHandleZeroCategoryId() {
            // Given
            Long categoryId = 0L;

            // When
            EventListFilter filter = new EventListFilter.Builder()
                .categoryId(categoryId)
                .build();

            // Then
            assertEquals(categoryId, filter.getCategoryId());
            assertTrue(filter.canFiltered());
        }

        @Test
        @DisplayName("categoryId가 음수인 경우 처리")
        void shouldHandleNegativeCategoryId() {
            // Given
            Long categoryId = -1L;

            // When
            EventListFilter filter = new EventListFilter.Builder()
                .categoryId(categoryId)
                .build();

            // Then
            assertEquals(categoryId, filter.getCategoryId());
            assertTrue(filter.canFiltered());
        }

        @Test
        @DisplayName("매우 큰 categoryId 처리")
        void shouldHandleLargeCategoryId() {
            // Given
            Long categoryId = Long.MAX_VALUE;

            // When
            EventListFilter filter = new EventListFilter.Builder()
                .categoryId(categoryId)
                .build();

            // Then
            assertEquals(categoryId, filter.getCategoryId());
            assertTrue(filter.canFiltered());
        }
    }

    // Helper methods
    private EventCategory createEventCategory(Long id) {
        // EventCategory는 Entity이므로 테스트에서는 mock 사용
        EventCategory category = mock(EventCategory.class);
        when(category.getId()).thenReturn(new CategoryId(id));
        return category;
    }

    private Location createLocation(String locationName) {
        return new Location(locationName, "홀A");
    }
}