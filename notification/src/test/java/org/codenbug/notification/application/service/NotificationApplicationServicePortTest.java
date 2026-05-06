package org.codenbug.notification.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.codenbug.notification.application.port.NotificationStore;
import org.codenbug.notification.domain.entity.Notification;
import org.codenbug.notification.domain.entity.NotificationType;
import org.codenbug.notification.domain.entity.UserId;
import org.codenbug.notification.domain.service.NotificationDomainService;
import org.codenbug.notification.infrastructure.NotificationStoreAdapter;
import org.codenbug.notification.infrastructure.NotificationRepository;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

class NotificationApplicationServicePortTest {

  @Test
  void 커맨드_서비스는_인프라_리포지토리가_아닌_저장소_포트에_의존한다() {
    assertThat(fieldTypes(NotificationCommandService.class))
        .doesNotContain(NotificationRepository.class)
        .contains(NotificationStore.class);
    assertThat(NotificationStore.class).isAssignableFrom(NotificationStoreAdapter.class);
  }

  @Test
  void 쿼리_서비스는_인프라_리포지토리가_아닌_저장소_포트에_의존한다() {
    assertThat(fieldTypes(NotificationQueryService.class))
        .doesNotContain(NotificationRepository.class)
        .contains(NotificationStore.class);
    assertThat(NotificationStore.class).isAssignableFrom(NotificationStoreAdapter.class);
  }

  @Test
  void 커맨드_서비스는_포트를_통해_알림을_저장하고_이벤트를_발행한다() {
    FakeNotificationStore store = new FakeNotificationStore();
    List<Object> publishedEvents = new ArrayList<>();
    ApplicationEventPublisher publisher = publishedEvents::add;
    NotificationCommandService service = new NotificationCommandService(store,
        new NotificationDomainService(), publisher);

    service.createNotification("user-1", NotificationType.SYSTEM, "제목", "내용", "/target");

    assertThat(store.savedNotifications).hasSize(1);
    assertThat(store.savedNotifications.get(0).getUserIdValue()).isEqualTo("user-1");
    assertThat(publishedEvents).hasSize(1);
  }

  @Test
  void 쿼리_서비스는_포트에서_조회한_알림을_읽음_처리하고_저장한다() {
    FakeNotificationStore store = new FakeNotificationStore();
    Notification notification = new NotificationDomainService()
        .createNotification("user-1", NotificationType.SYSTEM, "제목", "내용", null);
    store.notificationById = notification;
    NotificationQueryService service = new NotificationQueryService(store,
        new NotificationDomainService());

    service.getNotificationById(1L, "user-1");

    assertThat(notification.isRead()).isTrue();
    assertThat(store.savedNotifications).containsExactly(notification);
  }

  private List<Class<?>> fieldTypes(Class<?> type) {
    return List.of(type.getDeclaredFields()).stream().map(Field::getType).toList();
  }

  private static class FakeNotificationStore implements NotificationStore {

    private final List<Notification> savedNotifications = new ArrayList<>();
    private Notification notificationById;

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
    public Page<Notification> findByUserIdOrderBySentAtDesc(UserId userId,
        Pageable pageable) {
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
