package org.codenbug.notification.application.port;

import java.util.List;
import java.util.Optional;

import org.codenbug.notification.domain.entity.Notification;
import org.codenbug.notification.domain.entity.UserId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface NotificationStore {

    Notification save(Notification notification);

    Optional<Notification> findById(Long notificationId);

    Optional<Notification> findByIdAndUserId(Long notificationId, UserId userId);

    boolean existsBySourceKey(String sourceKey);

    Page<Notification> findByUserIdOrderBySentAtDesc(UserId userId, Pageable pageable);

    List<Notification> findByUserIdOrderBySentAtDesc(UserId userId);

    Page<Notification> findByUserIdAndIsReadFalseOrderBySentAtDesc(UserId userId,
            Pageable pageable);

    long countByUserIdAndIsReadFalse(UserId userId);

    default List<Notification> findAllByIdIn(List<Long> notificationIds) {
        throw new UnsupportedOperationException("ID 목록 알림 조회를 지원하지 않습니다.");
    }

    List<Notification> findAllByUserIdAndIdIn(UserId userId, List<Long> notificationIds);

    void delete(Notification notification);

    default void deleteAllByIdInBatch(List<Long> notificationIds) {
        throw new UnsupportedOperationException("ID 목록 알림 삭제를 지원하지 않습니다.");
    }

    void deleteAll(Iterable<? extends Notification> notifications);
}
