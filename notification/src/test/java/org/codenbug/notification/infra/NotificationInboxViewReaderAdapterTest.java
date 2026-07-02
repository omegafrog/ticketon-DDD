package org.codenbug.notification.infra;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;

import org.codenbug.notification.application.port.NotificationInboxViewReader;
import org.codenbug.notification.domain.NotificationDomainService;
import org.codenbug.notification.domain.entity.Notification;
import org.codenbug.notification.domain.entity.NotificationType;
import org.codenbug.notification.ui.projection.NotificationListProjection;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.util.ReflectionTestUtils;

import com.querydsl.jpa.impl.JPAQueryFactory;

import jakarta.annotation.Resource;
import jakarta.persistence.EntityManager;

@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=create-drop")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@ContextConfiguration(classes = NotificationInboxViewReaderAdapterTest.TestJpaApplication.class)
@Import({NotificationInboxViewReaderAdapter.class,
        NotificationInboxViewReaderAdapterTest.QuerydslTestConfig.class})
@EntityScan(basePackages = "org.codenbug.notification.domain.entity")
class NotificationInboxViewReaderAdapterTest {

    @Resource
    private EntityManager entityManager;

    @Resource
    private NotificationInboxViewReader notificationInboxViewReader;

    private final NotificationDomainService domainService = new NotificationDomainService();

    @BeforeEach
    void setUp() {
        persistNotification(1L, "user-1", LocalDateTime.of(2026, 6, 19, 10, 0), false);
        persistNotification(2L, "user-1", LocalDateTime.of(2026, 6, 19, 11, 0), true);
        persistNotification(3L, "user-1", LocalDateTime.of(2026, 6, 19, 12, 0), false);
        persistNotification(4L, "user-2", LocalDateTime.of(2026, 6, 19, 13, 0), false);
        entityManager.flush();
        entityManager.clear();
    }

    @Test
    void 사용자_알림목록은_최신순으로_타인알림없이_조회된다() {
        Page<NotificationListProjection> page = notificationInboxViewReader
                .findUserNotificationList("user-1", PageRequest.of(0, 10));

        assertThat(page.getContent()).hasSize(3);
        assertThat(notificationTitles(page)).containsExactly("title-3", "title-2", "title-1");
    }

    @Test
    void 미읽음_목록은_최신순이며_읽은항목과_타인알림을_제외한다() {
        Page<NotificationListProjection> page = notificationInboxViewReader
                .findUserUnreadNotificationList("user-1", PageRequest.of(0, 10));

        assertThat(page.getContent()).hasSize(2);
        assertThat(notificationTitles(page)).containsExactly("title-3", "title-1");
    }

    private void persistNotification(Long id, String userId, LocalDateTime sentAt, boolean isRead) {
        Notification notification = domainService.createNotification(userId, NotificationType.SYSTEM,
                "title-" + id, "content-" + id, "/target/" + id);
        if (isRead) {
            notification.markAsRead();
        }
        ReflectionTestUtils.setField(notification, "sentAt", sentAt);
        entityManager.persist(notification);
    }

    private java.util.List<String> notificationTitles(Page<NotificationListProjection> page) {
        return page.getContent().stream()
                .map(NotificationListProjection::getTitle)
                .toList();
    }

    @TestConfiguration
    static class QuerydslTestConfig {

        @Bean(name = "primaryQueryFactory")
        JPAQueryFactory primaryQueryFactory(EntityManager entityManager) {
            return new JPAQueryFactory(entityManager);
        }
    }

    @org.springframework.boot.SpringBootConfiguration
    @org.springframework.boot.autoconfigure.EnableAutoConfiguration
    @EntityScan(basePackages = "org.codenbug.notification.domain.entity")
    static class TestJpaApplication {
    }
}
