package org.codenbug.event.application;

import java.util.List;

public record PageOption(int page, List<SortOption> sortOptions) {
}
