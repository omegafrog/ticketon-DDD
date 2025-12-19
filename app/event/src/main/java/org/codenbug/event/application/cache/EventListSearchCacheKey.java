package org.codenbug.event.application.cache;

import java.util.Objects;
import org.codenbug.event.global.dto.EventListFilter;
import org.springframework.data.domain.Pageable;

public record EventListSearchCacheKey(EventListFilter filter, String keyword,
                                      PageOption pageOption) {

    public EventListSearchCacheKey(EventListFilter filter, String keyword, Pageable pageable) {
        this(filter, keyword,
            new PageOption(pageable.getPageNumber(), pageable.getSort().stream().map(sort -> {
                if (sort.getProperty().equals(SortMethod.DATETIME.columnName)) {
                    return new SortOption(SortMethod.DATETIME, sort.isAscending());
                }
                if (sort.getProperty().equals(SortMethod.VIEW_COUNT.columnName)) {
                    return new SortOption(SortMethod.VIEW_COUNT, sort.isAscending());
                }

                return new SortOption(SortMethod.DATETIME, sort.isAscending());
            }).toList()));
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof EventListSearchCacheKey cacheKey)) {
            return false;
        }
        return Objects.equals(keyword, cacheKey.keyword) && Objects.equals(
            pageOption, cacheKey.pageOption) && Objects.equals(filter, cacheKey.filter);
    }

    @Override
    public int hashCode() {
        return Objects.hash(filter, keyword, pageOption);
    }
}
