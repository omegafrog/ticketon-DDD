package org.codenbug.event.application.policy;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Field;
import java.time.LocalDate;
import java.util.List;
import org.codenbug.categoryid.domain.CategoryId;
import org.codenbug.categoryid.domain.EventCategory;
import org.codenbug.event.application.cache.EventListSearchCacheKey;
import org.codenbug.event.application.cache.PageOption;
import org.codenbug.event.application.cache.SortMethod;
import org.codenbug.event.application.cache.SortOption;
import org.codenbug.event.application.cache.policy.EventListSearchCacheablePolicyDispatcher;
import org.codenbug.event.domain.EventStatus;
import org.codenbug.event.global.dto.CostRange;
import org.codenbug.event.global.dto.EventListFilter;
import org.codenbug.seat.domain.RegionLocation;
import org.junit.jupiter.api.Test;

class EventListSearchCacheablePolicyDispatcherTest {

    private final EventListSearchCacheablePolicyDispatcher dispatcher =
        new EventListSearchCacheablePolicyDispatcher();
    private final Field filterField;
    private final Field keywordField;
    private final Field pageOptionField;

    EventListSearchCacheablePolicyDispatcherTest() throws NoSuchFieldException {
        this.filterField = EventListSearchCacheKey.class.getDeclaredField("filter");
        this.keywordField = EventListSearchCacheKey.class.getDeclaredField("keyword");
        this.pageOptionField = EventListSearchCacheKey.class.getDeclaredField("pageOption");
    }

    @Test
    void cacheableWhenFilterMatchesAllowedOptions() {
        EventListFilter filter = new EventListFilter.Builder()
            .startDate(LocalDate.now().plusDays(7).atStartOfDay())
            .regionLocationList(List.of(RegionLocation.SEOUL))
            .costRange(new CostRange(0, 10000))
            .eventCategoryList(List.of(createEventCategory(1L)))
            .eventStatusList(List.of(EventStatus.OPEN))
            .build();

        assertThat(dispatcher.isCacheable(filterField, filter)).isTrue();
    }

    @Test
    void notCacheableWhenFilterUsesUnsupportedStartDate() {
        EventListFilter filter = new EventListFilter.Builder()
            .startDate(LocalDate.now().minusDays(2).atStartOfDay())
            .regionLocationList(List.of(RegionLocation.SEOUL))
            .costRange(new CostRange(0, 10000))
            .eventCategoryList(List.of(createEventCategory(1L)))
            .eventStatusList(List.of(EventStatus.OPEN))
            .build();

        assertThat(dispatcher.isCacheable(filterField, filter)).isFalse();
    }

    @Test
    void cacheableOnlyWhenKeywordEmptyOrNull() {
        assertThat(dispatcher.isCacheable(keywordField, null)).isTrue();
        assertThat(dispatcher.isCacheable(keywordField, "")).isTrue();
        assertThat(dispatcher.isCacheable(keywordField, "concert")).isFalse();
    }

    @Test
    void cacheableWhenFilterIsNull() {
        assertThat(dispatcher.isCacheable(filterField, null)).isTrue();
    }

    @Test
    void cacheableForPageWithinWindowWithAllowedSortOptions() {
        PageOption pageOption = new PageOption(3,
            List.of(new SortOption(SortMethod.VIEW_COUNT, false),
                new SortOption(SortMethod.DATETIME, true),
                new SortOption(SortMethod.EVENT_START, true)));

        assertThat(dispatcher.isCacheable(pageOptionField, pageOption)).isTrue();
    }

    @Test
    void cacheableWhenPageOptionIsNull() {
        assertThat(dispatcher.isCacheable(pageOptionField, null)).isTrue();
    }

    @Test
    void notCacheableWhenPageExceedsCacheWindow() {
        PageOption pageOption = new PageOption(5,
            List.of(new SortOption(SortMethod.EVENT_START, true)));

        assertThat(dispatcher.isCacheable(pageOptionField, pageOption)).isFalse();
    }

    private EventCategory createEventCategory(long id) {
        EventCategory category = new EventCategory();
        try {
            Field idField = EventCategory.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(category, new CategoryId(id));
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
        return category;
    }
}
