package org.codenbug.nplus1;

import static org.assertj.core.api.Assertions.*;

import java.util.List;

import org.codenbug.notification.domain.entity.Notification;
import org.codenbug.notification.domain.entity.NotificationContent;
import org.codenbug.notification.domain.entity.NotificationType;
import org.codenbug.notification.domain.entity.UserId;
import org.codenbug.notification.infrastructure.NotificationRepository;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;

@DataJpaTest
@Import(NotificationNoNPlusOneTest.JpaTestConfig.class)
class NotificationNoNPlusOneTest {

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    @Test
    void findAll_thenAccessFields_doesNotTriggerExtraQueries() {
        for (int i = 0; i < 3; i++) {
            NotificationContent content = new NotificationContent("Title " + i, "Content " + i);
            Notification notification = new Notification(new UserId("user-" + i), NotificationType.PAYMENT, content);
            notificationRepository.save(notification);
        }

        entityManager.flush();

        Statistics statistics = getStatistics();
        statistics.clear();

        List<Notification> notifications = notificationRepository.findAll();
        long statementsAfterFindAll = statistics.getPrepareStatementCount();

        for (Notification notification : notifications) {
            notification.getTitle();
            notification.getContent();
        }

        long totalStatements = statistics.getPrepareStatementCount();

        assertThat(totalStatements)
            .as("Expected no extra queries because Notification has no relations")
            .isEqualTo(statementsAfterFindAll);
    }

    private Statistics getStatistics() {
        SessionFactory sessionFactory = entityManagerFactory.unwrap(SessionFactory.class);
        return sessionFactory.getStatistics();
    }

    @Configuration
    @EnableJpaAuditing
    @EntityScan(basePackages = "org.codenbug.notification.domain.entity")
    @EnableJpaRepositories(basePackages = "org.codenbug.notification.infrastructure")
    static class JpaTestConfig {
    }
}
