package org.codenbug.event.application.cache;

import java.util.List;
import java.util.Objects;

public record PageOption(int page, List<SortOption> sortOptions) {

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof PageOption that)) {
            return false;
        }
        return page == that.page && Objects.equals(sortOptions, that.sortOptions);
    }

    @Override
    public int hashCode() {
        return Objects.hash(page, sortOptions);
    }
}
