package org.codenbug.nplus1;

import static org.assertj.core.api.Assertions.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.codenbug.event.application.EventQueryService;
import org.codenbug.event.category.app.EventCategoryService;
import org.codenbug.event.category.domain.CategoryId;
import org.codenbug.event.category.domain.EventCategory;
import org.codenbug.event.category.infra.EventCategoryRepositoryImpl;
import org.codenbug.event.category.infra.JpaEventCategoryRepository;
import org.codenbug.event.domain.Event;
import org.codenbug.event.domain.EventCategoryId;
import org.codenbug.event.domain.EventInformation;
import org.codenbug.event.domain.EventRepository;
import org.codenbug.event.domain.EventStatus;
import org.codenbug.event.domain.ManagerId;
import org.codenbug.event.domain.MetaData;
import org.codenbug.event.infra.EventRepositoryImpl;
import org.codenbug.seat.domain.Location;
import org.codenbug.seat.domain.RegionLocation;
import org.codenbug.seat.domain.Seat;
import org.codenbug.seat.domain.SeatLayout;
import org.codenbug.seat.infra.JpaSeatRepository;
import org.codenbug.seat.infra.SeatLayoutRepositoryImpl;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.util.ReflectionTestUtils;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;

@DataJpaTest
@Import({
    EventRepositoryImpl.class,
    SeatLayoutRepositoryImpl.class,
    EventCategoryRepositoryImpl.class,
    EventCategoryService.class,
    EventQueryService.class
})
class EventManagerEventsNoNPlusOneTest {

    @Autowired
    private EventQueryService eventQueryService;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private JpaSeatRepository seatRepository;

    @Autowired
    private JpaEventCategoryRepository categoryRepository;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    @Test
    void getManagerEvents_fetchesSeatLayoutsInBatch() {
        int eventCount = 5;
        ManagerId managerId = new ManagerId("manager-1");
        LocalDateTime now = LocalDateTime.now();

        for (int i = 0; i < eventCount; i++) {
            EventCategory category = buildCategory((long) (i + 1));
            categoryRepository.save(category);

            SeatLayout layout = buildSeatLayout(i, 3);
            seatRepository.save(layout);

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
                new EventCategoryId(category.getId().getId())
            );

            Event event = new Event(info, managerId, layout.getId(), new MetaData());
            eventRepository.save(event);
        }

        entityManager.flush();
        entityManager.clear();

        Statistics statistics = getStatistics();
        statistics.clear();

        eventQueryService.getManagerEvents(managerId, PageRequest.of(0, 10));

        long totalStatements = statistics.getPrepareStatementCount();

        assertThat(totalStatements)
            .as("Expected a small fixed number of queries, not one per event")
            .isLessThanOrEqualTo(6L);
    }

    private EventCategory buildCategory(Long id) {
        EventCategory category = new EventCategory();
        ReflectionTestUtils.setField(category, "id", new CategoryId(id));
        ReflectionTestUtils.setField(category, "name", "Category " + id);
        ReflectionTestUtils.setField(category, "thumbnailUrl", "http://example.com/cat-" + id);
        return category;
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
    @EntityScan(basePackages = {
        "org.codenbug.event.domain",
        "org.codenbug.event.category.domain",
        "org.codenbug.seat.domain"
    })
    @EnableJpaRepositories(basePackages = {
        "org.codenbug.event.infra",
        "org.codenbug.event.category.infra",
        "org.codenbug.seat.infra"
    })
    static class JpaTestConfig {
    }
}
