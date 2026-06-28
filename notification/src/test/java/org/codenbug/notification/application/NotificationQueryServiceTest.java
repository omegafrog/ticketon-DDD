package org.codenbug.notification.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.codenbug.notification.application.port.NotificationStore;
import org.codenbug.notification.domain.entity.Notification;
import org.codenbug.notification.domain.entity.NotificationType;
import org.codenbug.notification.domain.entity.UserId;
import org.codenbug.notification.domain.NotificationDomainService;
import org.codenbug.notification.ui.projection.NotificationListProjection;
import org.codenbug.notification.ui.repository.NotificationViewRepository;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

class NotificationQueryServiceTest {

    private final NotificationDomainService domainService = new NotificationDomainService();

    @Test
    void 목록_조회는_저장없이_리시피언트_범위로만_조회한다() {
        FakeNotificationStore store = new FakeNotificationStore();
        FakeNotificationViewRepository viewRepository = new FakeNotificationViewRepository();
        PageRequest pageable = PageRequest.of(0, 10);
        viewRepository.notificationsPage = new PageImpl<>(List.of(
                projection(1L, "user-1", false, LocalDateTime.now())), pageable, 1);
        NotificationQueryService service =
                new NotificationQueryService(store, viewRepository, domainService);

        Page<NotificationListProjection> result = service.getNotifications("user-1", pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(viewRepository.lastNotificationUserId).isEqualTo("user-1");
        assertThat(store.savedNotifications).isEmpty();
    }

    @Test
    void 미읽음_개수는_저장없이_리시피언트_범위만_반영한다() {
        FakeNotificationStore store = new FakeNotificationStore();
        FakeNotificationViewRepository viewRepository = new FakeNotificationViewRepository();
        store.unreadCount = 3L;
        NotificationQueryService service =
                new NotificationQueryService(store, viewRepository, domainService);

        long count = service.getUnreadCount("user-1");

        assertThat(count).isEqualTo(3L);
        assertThat(store.lastUnreadCountUserId).isEqualTo(new UserId("user-1"));
        assertThat(store.savedNotifications).isEmpty();
    }

    @Test
    void 미읽음_목록_조회는_저장없이_리시피언트_범위로만_조회한다() {
        FakeNotificationStore store = new FakeNotificationStore();
        FakeNotificationViewRepository viewRepository = new FakeNotificationViewRepository();
        PageRequest pageable = PageRequest.of(0, 20);
        viewRepository.unreadPage = new PageImpl<>(List.of(
                projection(2L, "user-1", false, LocalDateTime.now())), pageable, 1);
        NotificationQueryService service =
                new NotificationQueryService(store, viewRepository, domainService);

        Page<NotificationListProjection> result = service.getUnreadNotifications("user-1", pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(viewRepository.lastUnreadUserId).isEqualTo("user-1");
        assertThat(store.savedNotifications).isEmpty();
    }

    @Test
    void 상세_조회는_소유한_알림만_읽음처리한다() {
        FakeNotificationStore store = new FakeNotificationStore();
        FakeNotificationViewRepository viewRepository = new FakeNotificationViewRepository();
        Notification notification =
                domainService.createNotification("user-1", NotificationType.SYSTEM, "제목", "내용", null);
        store.notificationById = notification;
        NotificationQueryService service =
                new NotificationQueryService(store, viewRepository, domainService);

        service.getNotificationById(1L, "user-1");

        assertThat(notification.isRead()).isTrue();
        assertThat(store.savedNotifications).containsExactly(notification);
    }

    @Test
    void 이미_읽은_상세_재조회는_추가_저장을_만들지_않는다() {
        FakeNotificationStore store = new FakeNotificationStore();
        FakeNotificationViewRepository viewRepository = new FakeNotificationViewRepository();
        Notification notification =
                domainService.createNotification("user-1", NotificationType.SYSTEM, "제목", "내용", null);
        notification.markAsRead();
        store.notificationById = notification;
        NotificationQueryService service =
                new NotificationQueryService(store, viewRepository, domainService);

        service.getNotificationById(1L, "user-1");

        assertThat(notification.isRead()).isTrue();
        assertThat(store.savedNotifications).isEmpty();
    }

    @Test
    void 타인_알림_상세_조회는_읽음변경없이_실패한다() {
        FakeNotificationStore store = new FakeNotificationStore();
        FakeNotificationViewRepository viewRepository = new FakeNotificationViewRepository();
        Notification notification =
                domainService.createNotification("user-2", NotificationType.SYSTEM, "제목", "내용", null);
        store.notificationById = notification;
        NotificationQueryService service =
                new NotificationQueryService(store, viewRepository, domainService);

        assertThatThrownBy(() -> service.getNotificationById(1L, "user-1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("해당 알림에 접근할 권한이 없습니다.");
        assertThat(notification.isRead()).isFalse();
        assertThat(store.savedNotifications).isEmpty();
    }

    @Test
    void 없는_알림_상세_조회는_저장없이_실패한다() {
        FakeNotificationStore store = new FakeNotificationStore();
        FakeNotificationViewRepository viewRepository = new FakeNotificationViewRepository();
        NotificationQueryService service =
                new NotificationQueryService(store, viewRepository, domainService);

        assertThatThrownBy(() -> service.getNotificationById(99L, "user-1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("해당 알림을 찾을 수 없습니다.");
        assertThat(store.savedNotifications).isEmpty();
    }

    private NotificationListProjection projection(Long id, String userId, boolean isRead,
            LocalDateTime sentAt) {
        return new NotificationListProjection(id, userId, "title-" + id, "content-" + id,
                NotificationType.SYSTEM, isRead, "PENDING", sentAt);
    }

    private static class FakeNotificationStore implements NotificationStore {

        private final List<Notification> savedNotifications = new ArrayList<>();
        private Notification notificationById;
        private long unreadCount;
        private UserId lastUnreadCountUserId;

        @Override
        public Notification save(Notification notification) {
            savedNotifications.add(notification);
            return notification;
        }

        @Override
        public Optional<Notification> findById(Long notificationId) {
            return Optional.ofNullable(notificationById);
        }

        @Override
        public boolean existsBySourceKey(String sourceKey) {
            return false;
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
            lastUnreadCountUserId = userId;
            return unreadCount;
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

    private static class FakeNotificationViewRepository implements NotificationViewRepository {

        private Page<NotificationListProjection> notificationsPage = Page.empty();
        private Page<NotificationListProjection> unreadPage = Page.empty();
        private String lastNotificationUserId;
        private String lastUnreadUserId;

        @Override
        public Page<NotificationListProjection> findUserNotificationList(String userId,
                Pageable pageable) {
            lastNotificationUserId = userId;
            return notificationsPage;
        }

        @Override
        public Page<NotificationListProjection> findUserUnreadNotificationList(String userId,
                Pageable pageable) {
            lastUnreadUserId = userId;
            return unreadPage;
        }

        @Override
        public Page<NotificationListProjection> findUserNotificationListByType(String userId,
                String type, Pageable pageable) {
            return Page.empty();
        }

        @Override
        public List<NotificationListProjection> findUserNotificationListWithCursor(String userId,
                Long cursor, int size) {
            return List.of();
        }

        @Override
        public long countUnreadNotifications(String userId) {
            return 0;
        }
    }
}
