package org.codenbug.notification.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.codenbug.notification.application.port.NotificationStore;
import org.codenbug.notification.domain.entity.Notification;
import org.codenbug.notification.domain.entity.NotificationType;
import org.codenbug.notification.domain.entity.UserId;
import org.codenbug.notification.domain.NotificationDomainService;
import org.codenbug.notification.domain.NotificationDeletionPolicy;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

class NotificationCommandServiceIdempotencyTest {

    @Test
    void sourceKey가_이미_존재하면_알림을_다시_저장하지_않는다() {
        FakeNotificationStore store = new FakeNotificationStore();
        store.existingSourceKeys.add("payment.completed:user-1:purchase-1:2026-06-19T10:15:30");
        List<Object> publishedEvents = new ArrayList<>();
        ApplicationEventPublisher publisher = publishedEvents::add;
        NotificationCommandService service = new NotificationCommandService(store,
                new NotificationDomainService(), new NotificationDeletionPolicy(), publisher);

        Optional<?> result = service.createNotificationIfAbsent("user-1", NotificationType.PAYMENT,
                "제목", "내용", "/target",
                "payment.completed:user-1:purchase-1:2026-06-19T10:15:30");

        assertThat(result).isEmpty();
        assertThat(store.savedNotifications).isEmpty();
        assertThat(publishedEvents).isEmpty();
    }

    @Test
    void sourceKey가_없으면_알림을_저장하고_이벤트를_발행한다() {
        FakeNotificationStore store = new FakeNotificationStore();
        List<Object> publishedEvents = new ArrayList<>();
        ApplicationEventPublisher publisher = publishedEvents::add;
        NotificationCommandService service = new NotificationCommandService(store,
                new NotificationDomainService(), new NotificationDeletionPolicy(), publisher);

        Optional<?> result = service.createNotificationIfAbsent("user-1", NotificationType.PAYMENT,
                "제목", "내용", "/target",
                "payment.completed:user-1:purchase-1:2026-06-19T10:15:30");

        assertThat(result).isPresent();
        assertThat(store.savedNotifications).hasSize(1);
        assertThat(store.savedNotifications.get(0).getSourceKey())
                .isEqualTo("payment.completed:user-1:purchase-1:2026-06-19T10:15:30");
        assertThat(publishedEvents).hasSize(1);
    }

    private static class FakeNotificationStore implements NotificationStore {

        private final List<String> existingSourceKeys = new ArrayList<>();
        private final List<Notification> savedNotifications = new ArrayList<>();

        @Override
        public Notification save(Notification notification) {
            savedNotifications.add(notification);
            return notification;
        }

        @Override
        public Optional<Notification> findById(Long notificationId) {
            return Optional.empty();
        }

        @Override
        public Optional<Notification> findByIdAndUserId(Long notificationId, UserId userId) {
            return Optional.empty();
        }

        @Override
        public boolean existsBySourceKey(String sourceKey) {
            return existingSourceKeys.contains(sourceKey);
        }

        @Override
        public Page<Notification> findByUserIdOrderBySentAtDesc(UserId userId, Pageable pageable) {
            return new PageImpl<>(List.of(), pageable, 0);
        }

        @Override
        public List<Notification> findByUserIdOrderBySentAtDesc(UserId userId) {
            return List.of();
        }

        @Override
        public Page<Notification> findByUserIdAndIsReadFalseOrderBySentAtDesc(UserId userId,
                Pageable pageable) {
            return new PageImpl<>(List.of(), pageable, 0);
        }

        @Override
        public long countByUserIdAndIsReadFalse(UserId userId) {
            return 0;
        }

        @Override
        public List<Notification> findAllByUserIdAndIdIn(UserId userId,
                List<Long> notificationIds) {
            return List.of();
        }

        @Override
        public void delete(Notification notification) {
        }

        @Override
        public void deleteAll(Iterable<? extends Notification> notifications) {
        }
    }
}
