package org.codenbug.nplus1;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.jdbc.Sql;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@DataJpaTest
@Import(SeedSqlDataCompatibilityTest.JpaTestConfig.class)
@Sql("/sql/seed_event_seat_layout_seat_h2.sql")
class SeedSqlDataCompatibilityTest {

    @PersistenceContext
    private EntityManager entityManager;

    @Test
    void seedSql_insertsExpectedRowCounts() {
        Long eventCount = entityManager.createQuery("select count(e) from Event e", Long.class)
            .getSingleResult();
        Long seatLayoutCount = entityManager.createQuery("select count(sl) from SeatLayout sl", Long.class)
            .getSingleResult();
        Long seatCount = entityManager.createQuery("select count(s) from Seat s", Long.class)
            .getSingleResult();

        assertThat(eventCount).isEqualTo(2L);
        assertThat(seatLayoutCount).isEqualTo(2L);
        assertThat(seatCount).isEqualTo(10L);
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
    }
}
