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

    Page<Notification> findByUserIdOrderBySentAtDesc(UserId userId, Pageable pageable);

    List<Notification> findByUserIdOrderBySentAtDesc(UserId userId);

    Page<Notification> findByUserIdAndIsReadFalseOrderBySentAtDesc(UserId userId,
            Pageable pageable);

    long countByUserIdAndIsReadFalse(UserId userId);

    List<Notification> findAllByUserIdAndIdIn(UserId userId, List<Long> notificationIds);

    void delete(Notification notification);

    void deleteAll(Iterable<? extends Notification> notifications);
}
