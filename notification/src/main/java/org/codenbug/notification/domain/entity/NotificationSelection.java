package org.codenbug.notification.domain.entity;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;

import lombok.EqualsAndHashCode;
import lombok.Getter;

@Getter
@EqualsAndHashCode
public class NotificationSelection {

    private final List<Long> notificationIds;

    private NotificationSelection(List<Long> notificationIds) {
        this.notificationIds = List.copyOf(notificationIds);
    }

    public static NotificationSelection from(List<Long> notificationIds) {
        Objects.requireNonNull(notificationIds, "삭제할 알림 ID 목록은 필수입니다.");

        List<Long> normalizedIds = notificationIds.stream()
                .map(notificationId -> Objects.requireNonNull(notificationId, "알림 ID는 필수입니다."))
                .collect(java.util.stream.Collectors.collectingAndThen(
                        java.util.stream.Collectors.toCollection(LinkedHashSet::new), List::copyOf));

        if (normalizedIds.isEmpty()) {
            throw new IllegalArgumentException("삭제할 알림 ID 목록은 비어 있을 수 없습니다.");
        }

        return new NotificationSelection(normalizedIds);
    }

    public int size() {
        return notificationIds.size();
    }
}
