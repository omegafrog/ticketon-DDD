package org.codenbug.event.application;

import org.codenbug.event.global.EventListFilter;
import org.springframework.data.domain.Pageable;

public record EventListSearchCacheKey(EventListFilter filter, String keyword, PageOption pageOption) {
    public EventListSearchCacheKey(EventListFilter filter, String keyword, Pageable pageable){
        this(filter, keyword, new PageOption(pageable.getPageNumber(), pageable.getSort().stream().map(sort -> {
            if (sort.getProperty().equals(SortMethod.DATETIME.columnName)) {
                return new SortOption(SortMethod.DATETIME, sort.isAscending());
            }
            if (sort.getProperty().equals(SortMethod.VIEW_COUNT.columnName)) {
                return new SortOption(SortMethod.VIEW_COUNT, sort.isAscending());
            }

            return new SortOption(SortMethod.DATETIME, sort.isAscending());
        }).toList()));
    }
}
