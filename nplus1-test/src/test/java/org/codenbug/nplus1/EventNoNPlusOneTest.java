package org.codenbug.nplus1;

import static org.assertj.core.api.Assertions.*;

import java.time.LocalDateTime;
import java.util.List;

import org.codenbug.event.domain.Event;
import org.codenbug.event.domain.EventCategoryId;
import org.codenbug.event.domain.EventInformation;
import org.codenbug.event.domain.EventStatus;
import org.codenbug.event.domain.ManagerId;
import org.codenbug.event.domain.MetaData;
import org.codenbug.event.infra.JpaEventRepository;
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
@Import(EventNoNPlusOneTest.JpaTestConfig.class)
class EventNoNPlusOneTest {

    @Autowired
    private JpaEventRepository eventRepository;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    @Test
    void findAll_thenAccessFields_doesNotTriggerExtraQueries() {
        LocalDateTime now = LocalDateTime.now();

        for (int i = 0; i < 3; i++) {
            EventInformation info = new EventInformation(
                "Event " + i,
                "http://example.com/" + i,
                0,
                "restrictions",
                "description",
                now.minusDays(10),
                now.minusDays(5),
                now.plusDays(1),
                now.plusDays(2),
                false,
                1000,
                2000,
                EventStatus.CLOSED,
                new EventCategoryId(1L)
            );
            Event event = new Event(info, new ManagerId("manager-" + i), 1L, new MetaData());
            eventRepository.save(event);
        }

        entityManager.flush();
        entityManager.clear();

        Statistics statistics = getStatistics();
        statistics.clear();

        List<Event> events = eventRepository.findAll();
        long statementsAfterFindAll = statistics.getPrepareStatementCount();

        for (Event event : events) {
            event.getEventInformation();
            event.getManagerId();
        }

        long totalStatements = statistics.getPrepareStatementCount();

        assertThat(totalStatements)
            .as("Expected no extra queries because Event has no relations")
            .isEqualTo(statementsAfterFindAll);
    }

    private Statistics getStatistics() {
        SessionFactory sessionFactory = entityManagerFactory.unwrap(SessionFactory.class);
        return sessionFactory.getStatistics();
    }

    @Configuration
    @EnableJpaAuditing
    @EntityScan(basePackages = "org.codenbug.event.domain")
    @EnableJpaRepositories(basePackages = "org.codenbug.event.infra")
    static class JpaTestConfig {
    }
}
