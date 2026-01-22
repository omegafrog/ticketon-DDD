package org.codenbug.nplus1;

import static org.assertj.core.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.codenbug.seat.domain.Location;
import org.codenbug.seat.domain.RegionLocation;
import org.codenbug.seat.domain.Seat;
import org.codenbug.seat.domain.SeatLayout;
import org.codenbug.seat.infra.JpaSeatRepository;
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
@Import(SeatNPlusOneTest.JpaTestConfig.class)
class SeatNPlusOneTest {

    @Autowired
    private JpaSeatRepository seatRepository;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    @Test
    void findAll_thenAccessSeats_triggersNPlusOne() {
        int layoutCount = 3;
        int seatsPerLayout = 4;

        for (int i = 0; i < layoutCount; i++) {
            SeatLayout layout = buildSeatLayout(i, seatsPerLayout);
            seatRepository.save(layout);
        }

        entityManager.flush();
        entityManager.clear();

        Statistics statistics = getStatistics();
        statistics.clear();

        List<SeatLayout> layouts = seatRepository.findAll();
        long statementsAfterFindAll = statistics.getPrepareStatementCount();

        for (SeatLayout layout : layouts) {
            Set<Seat> seats = layout.getSeats();
            seats.size();
        }

        long totalStatements = statistics.getPrepareStatementCount();
        long extraStatements = totalStatements - statementsAfterFindAll;

        assertThat(extraStatements)
            .as("Expected at least one extra query per seat layout when loading seats lazily")
            .isGreaterThanOrEqualTo(layoutCount);
    }

    private SeatLayout buildSeatLayout(int index, int seatsPerLayout) {
        List<List<String>> layout = new ArrayList<>();
        List<Seat> seats = new ArrayList<>();

        int created = 0;
        int rowIndex = 0;
        while (created < seatsPerLayout) {
            List<String> row = new ArrayList<>();
            for (int col = 0; col < 2 && created < seatsPerLayout; col++) {
                String signature = "R" + rowIndex + "C" + col + "-" + index;
                row.add(signature);
                seats.add(new Seat(signature, 1000 + created, "S"));
                created++;
            }
            layout.add(row);
            rowIndex++;
        }

        Location location = new Location("Venue-" + index, "Hall-" + index);
        return new SeatLayout(layout, location, RegionLocation.SEOUL, seats);
    }

    private Statistics getStatistics() {
        SessionFactory sessionFactory = entityManagerFactory.unwrap(SessionFactory.class);
        return sessionFactory.getStatistics();
    }

    @Configuration
    @EnableJpaAuditing
    @EntityScan(basePackages = "org.codenbug.seat.domain")
    @EnableJpaRepositories(basePackages = "org.codenbug.seat.infra")
    static class JpaTestConfig {
    }
}
