package org.codenbug.categoryid.global;

import lombok.Getter;

@Getter
public class EventCategoryListResponse {
    private final Long categoryId;
    private final String name;

    public EventCategoryListResponse(Long categoryId, String name) {
        this.categoryId = categoryId;
        this.name = name;
    }
}
