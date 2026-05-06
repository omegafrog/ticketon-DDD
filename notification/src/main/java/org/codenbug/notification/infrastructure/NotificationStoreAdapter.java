package org.codenbug.notification.infrastructure;

import java.util.List;
import java.util.Optional;

import lombok.RequiredArgsConstructor;
import org.codenbug.notification.application.port.NotificationStore;
import org.codenbug.notification.domain.entity.Notification;
import org.codenbug.notification.domain.entity.UserId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class NotificationStoreAdapter implements NotificationStore {

    private final NotificationRepository notificationRepository;

    @Override
    public Notification save(Notification notification) {
        return notificationRepository.save(notification);
    }

    @Override
    public Optional<Notification> findById(Long notificationId) {
        return notificationRepository.findById(notificationId);
    }

    @Override
    public Page<Notification> findByUserIdOrderBySentAtDesc(UserId userId, Pageable pageable) {
        return notificationRepository.findByUserIdOrderBySentAtDesc(userId, pageable);
    }

    @Override
    public List<Notification> findByUserIdOrderBySentAtDesc(UserId userId) {
        return notificationRepository.findByUserIdOrderBySentAtDesc(userId);
    }

    @Override
    public Page<Notification> findByUserIdAndIsReadFalseOrderBySentAtDesc(UserId userId,
            Pageable pageable) {
        return notificationRepository.findByUserIdAndIsReadFalseOrderBySentAtDesc(userId,
                pageable);
    }

    @Override
    public long countByUserIdAndIsReadFalse(UserId userId) {
        return notificationRepository.countByUserIdAndIsReadFalse(userId);
    }

    @Override
    public List<Notification> findAllByUserIdAndIdIn(UserId userId,
            List<Long> notificationIds) {
        return notificationRepository.findAllByUserIdAndIdIn(userId, notificationIds);
    }

    @Override
    public void delete(Notification notification) {
        notificationRepository.delete(notification);
    }

    @Override
    public void deleteAll(Iterable<? extends Notification> notifications) {
        notificationRepository.deleteAll(notifications);
    }
}
