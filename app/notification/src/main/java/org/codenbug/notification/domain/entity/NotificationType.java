package org.codenbug.notification.domain.entity;

public enum NotificationType {
    SYSTEM("시스템 공지 관련 알림"), EVENT("이벤트, 공연 관련 알림"), TICKET("티켓 예매/취소 관련 알림"), PAYMENT(
            "결제 완료, 환불 등 결제 관련 알림");

    private final String description;

    NotificationType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
