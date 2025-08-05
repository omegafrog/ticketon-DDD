package org.codenbug.notification.domain.entity;

public enum NotificationStatus {
    PENDING("생성됐지만 아직 전송되지 않음"),
    SENT("성공적으로 전송됨"),
    FAILED("전송 실패");

    private final String description;

    NotificationStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}