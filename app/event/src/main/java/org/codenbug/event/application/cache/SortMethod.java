package org.codenbug.event.application.cache;

public enum SortMethod {
    VIEW_COUNT("viewCount"), DATETIME("createdAt"), EVENT_START("eventStart");
    public String columnName;

    SortMethod(String columnName) {
        this.columnName = columnName;
    }
}
