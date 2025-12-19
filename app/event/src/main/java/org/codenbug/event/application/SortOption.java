package org.codenbug.event.application;

import java.util.Objects;

public record SortOption(SortMethod sortMethod, boolean asc) {

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof SortOption that)) {
            return false;
        }
        return asc == that.asc && sortMethod == that.sortMethod;
    }

    @Override
    public int hashCode() {
        return Objects.hash(sortMethod, asc);
    }
}
