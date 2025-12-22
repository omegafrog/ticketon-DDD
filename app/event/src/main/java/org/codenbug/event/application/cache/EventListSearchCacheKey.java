package org.codenbug.event.application.cache;

import java.util.Objects;
import org.codenbug.cachecore.event.search.CacheKeyWithEpoch;
import org.codenbug.event.application.dto.EventListFilter;
import org.springframework.data.domain.Pageable;

public class EventListSearchCacheKey extends CacheKeyWithEpoch {

    private final EventListFilter filter;
    private final String keyword;
    private final PageOption pageOption;

    public EventListSearchCacheKey(long epoch, EventListFilter filter, String keyword,
        PageOption pageOption) {
        super(epoch);
        this.filter = filter;
        this.keyword = keyword;
        this.pageOption = pageOption;
    }

    public EventListSearchCacheKey(long epoch, EventListFilter filter, String keyword,
        Pageable pageable) {
        this(epoch, filter, keyword, new PageOption(pageable.getPageNumber(),
            pageable.getSort().stream().map(sort -> {
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
        return getEpoch() == cacheKey.getEpoch()
            && Objects.equals(keyword, cacheKey.keyword)
            && Objects.equals(pageOption, cacheKey.pageOption)
            && Objects.equals(filter, cacheKey.filter);
    }

    @Override
    public int hashCode() {
        return Objects.hash(getEpoch(), filter, keyword, pageOption);
    }

    @Override
    public String toString() {
        return "EventListSearchCacheKey{" +
            "filter=" + filter +
            ", keyword='" + keyword + '\'' +
            ", pageOption=" + pageOption +
            '}';
    }
}
