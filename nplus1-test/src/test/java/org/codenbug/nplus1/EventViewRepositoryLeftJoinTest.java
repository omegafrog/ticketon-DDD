package org.codenbug.nplus1;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

import org.codenbug.event.infra.EventViewRepositoryImpl;
import org.codenbug.event.query.EventListProjection;
import org.codenbug.event.query.EventViewRepository;
import org.codenbug.event.query.RedisViewCountService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.PlatformTransactionManager;

import com.querydsl.jpa.impl.JPAQueryFactory;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;

@DataJpaTest
@Import({
    EventViewRepositoryImpl.class,
    EventViewRepositoryLeftJoinTest.JpaTestConfig.class
})
@Sql("/sql/seed_event_seat_layout_seat_h2.sql")
class EventViewRepositoryLeftJoinTest {

    @Autowired
    private EventViewRepository eventViewRepository;

    @MockBean
    private RedisViewCountService redisViewCountService;

    @Test
    void findEventList_doesNotDropEvents_whenSeatLayoutStatsMissing() {
        given(redisViewCountService.getViewCountForList(anyString(), any())).willAnswer(inv -> inv.getArgument(1));

        Page<EventListProjection> page = eventViewRepository.findEventList(null, null, PageRequest.of(0, 10));

        assertThat(page.getTotalElements()).isEqualTo(2L);
        assertThat(page.getContent()).hasSize(2);

        // ordered by createdAt desc in repository default
        EventListProjection first = page.getContent().get(0);
        EventListProjection second = page.getContent().get(1);

        assertThat(first.getEventId()).isEqualTo("evt-1002");
        assertThat(second.getEventId()).isEqualTo("evt-1001");

        // seat_layout_stats is intentionally empty -> seatCount coalesces to 0, prices come from event table
        assertThat(first.getSeatCount()).isEqualTo(0L);
        assertThat(second.getSeatCount()).isEqualTo(0L);

        assertThat(first.getMinPrice()).isEqualTo(40000);
        assertThat(first.getMaxPrice()).isEqualTo(120000);
        assertThat(second.getMinPrice()).isEqualTo(50000);
        assertThat(second.getMaxPrice()).isEqualTo(100000);
    }

    @Configuration
    @EnableJpaAuditing
    @EntityScan(basePackages = {
        "org.codenbug.event.domain",
        "org.codenbug.seat.domain"
    })
    @EnableJpaRepositories(basePackages = {
        "org.codenbug.event.infra",
        "org.codenbug.seat.infra"
    })
    static class JpaTestConfig {

        @Bean(name = "readOnlyQueryFactory")
        JPAQueryFactory readOnlyQueryFactory(EntityManager entityManager) {
            return new JPAQueryFactory(entityManager);
        }

        @Bean(name = "readOnlyTransactionManager")
        PlatformTransactionManager readOnlyTransactionManager(EntityManagerFactory entityManagerFactory) {
            return new JpaTransactionManager(entityManagerFactory);
        }
    }
}
