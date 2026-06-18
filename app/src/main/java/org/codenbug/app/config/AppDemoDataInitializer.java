package org.codenbug.app.config;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.codenbug.event.category.domain.CategoryId;
import org.codenbug.event.category.domain.EventCategory;
import org.codenbug.event.category.infra.JpaEventCategoryRepository;
import org.codenbug.event.domain.Event;
import org.codenbug.event.domain.EventCategoryId;
import org.codenbug.event.domain.EventInformation;
import org.codenbug.event.domain.EventStatus;
import org.codenbug.event.domain.ManagerId;
import org.codenbug.event.domain.MetaData;
import org.codenbug.event.infra.JpaEventRepository;
import org.codenbug.seat.domain.Location;
import org.codenbug.seat.domain.RegionLocation;
import org.codenbug.seat.domain.Seat;
import org.codenbug.seat.domain.SeatLayout;
import org.codenbug.seat.infra.JpaSeatRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@ConditionalOnProperty(name = "ticketon.demo-data.enabled", havingValue = "true")
public class AppDemoDataInitializer implements ApplicationRunner {
	private static final String MANAGER_ID = "demo-manager";

	private final JpaEventCategoryRepository categoryRepository;
	private final JpaSeatRepository seatRepository;
	private final JpaEventRepository eventRepository;
	private final EntityManager entityManager;

	public AppDemoDataInitializer(
		JpaEventCategoryRepository categoryRepository,
		JpaSeatRepository seatRepository,
		JpaEventRepository eventRepository,
		EntityManager entityManager) {
		this.categoryRepository = categoryRepository;
		this.seatRepository = seatRepository;
		this.eventRepository = eventRepository;
		this.entityManager = entityManager;
	}

	@Override
	@Transactional
	public void run(ApplicationArguments args) {
		seedCategories();
		if (eventRepository.count() > 0) {
			return;
		}

		List<DemoEvent> demoEvents = List.of(
			new DemoEvent("TicketOn Live Night", 1L, RegionLocation.SEOUL, "Seoul Arts Center", "Main Hall",
				"https://images.unsplash.com/photo-1493225457124-a3eb161ffa5f?auto=format&fit=crop&w=1200&q=80",
				"Reserved-seat live concert for end-to-end booking tests."),
			new DemoEvent("Blue Ocean Musical", 2L, RegionLocation.BUSAN, "Busan Dream Arena", "Blue Stage",
				"https://images.unsplash.com/photo-1503095396549-807759245b35?auto=format&fit=crop&w=1200&q=80",
				"Demo musical with visible seat grades and prices."),
			new DemoEvent("Modern Light Exhibition", 3L, RegionLocation.SEOUL, "DDP", "Design Hall",
				"https://images.unsplash.com/photo-1531058020387-3be344556be6?auto=format&fit=crop&w=1200&q=80",
				"Immersive exhibition sample event."),
			new DemoEvent("City Finals 2026", 4L, RegionLocation.INCHEON, "Incheon Arena", "Court A",
				"https://images.unsplash.com/photo-1461896836934-ffe607ba8211?auto=format&fit=crop&w=1200&q=80",
				"Sports ticket sample with selectable seats."),
			new DemoEvent("Star Fan Meeting", 5L, RegionLocation.DAEGU, "Daegu Hall", "Grand Room",
				"https://images.unsplash.com/photo-1516280440614-37939bbacd81?auto=format&fit=crop&w=1200&q=80",
				"Fan meeting sample event."),
			new DemoEvent("Weekend Culture Pass", 6L, RegionLocation.DAEJEON, "Daejeon Culture Center", "Room 1",
				"https://images.unsplash.com/photo-1517457373958-b7bdd4587205?auto=format&fit=crop&w=1200&q=80",
				"General category sample event."));

		for (int i = 0; i < demoEvents.size(); i++) {
			seedEvent(demoEvents.get(i), i);
		}
		log.info("Seeded {} demo events", demoEvents.size());
	}

	private void seedCategories() {
		List<EventCategory> categories = List.of(
			new EventCategory(new CategoryId(1L), "CONCERT",
				"https://images.unsplash.com/photo-1501281668745-f7f57925c3b4?auto=format&fit=crop&w=800&q=80"),
			new EventCategory(new CategoryId(2L), "MUSICAL",
				"https://images.unsplash.com/photo-1503095396549-807759245b35?auto=format&fit=crop&w=800&q=80"),
			new EventCategory(new CategoryId(3L), "EXHIBITION",
				"https://images.unsplash.com/photo-1531058020387-3be344556be6?auto=format&fit=crop&w=800&q=80"),
			new EventCategory(new CategoryId(4L), "SPORTS",
				"https://images.unsplash.com/photo-1461896836934-ffe607ba8211?auto=format&fit=crop&w=800&q=80"),
			new EventCategory(new CategoryId(5L), "FAN_MEETING",
				"https://images.unsplash.com/photo-1516280440614-37939bbacd81?auto=format&fit=crop&w=800&q=80"),
			new EventCategory(new CategoryId(6L), "ETC",
				"https://images.unsplash.com/photo-1517457373958-b7bdd4587205?auto=format&fit=crop&w=800&q=80"));
		categoryRepository.saveAll(categories);
	}

	private void seedEvent(DemoEvent demo, int offset) {
		SeatLayout layout = seatRepository.save(new SeatLayout(
			seatMatrix(),
			new Location(demo.locationName(), demo.hallName()),
			demo.regionLocation(),
			seats()));
		upsertSeatStats(layout.getId(), 12, 70000, 150000);

		LocalDateTime now = LocalDateTime.now();
		LocalDateTime bookingStart = now.minusDays(1);
		LocalDateTime bookingEnd = now.plusDays(45 + offset);
		LocalDateTime eventStart = bookingEnd.plusDays(5).withHour(19).withMinute(30).withSecond(0).withNano(0);
		LocalDateTime eventEnd = eventStart.plusHours(2);

		EventInformation information = new EventInformation(
			demo.title(),
			demo.thumbnailUrl(),
			0,
			"Demo event. No outside food.",
			demo.description(),
			bookingStart,
			bookingEnd,
			eventStart,
			eventEnd,
			true,
			70000,
			150000,
			EventStatus.OPEN,
			new EventCategoryId(demo.categoryId()));

		eventRepository.save(new Event(information, new ManagerId(MANAGER_ID), layout.getId(), new MetaData()));
	}

	private List<List<String>> seatMatrix() {
		return List.of(
			List.of("A1", "A2", "A3", "A4"),
			List.of("B1", "B2", "B3", "B4"),
			List.of("C1", "C2", "C3", "C4"));
	}

	private List<Seat> seats() {
		List<Seat> seats = new ArrayList<>();
		for (String signature : List.of("A1", "A2", "A3", "A4")) {
			seats.add(new Seat(signature, 150000, "VIP"));
		}
		for (String signature : List.of("B1", "B2", "B3", "B4")) {
			seats.add(new Seat(signature, 100000, "R"));
		}
		for (String signature : List.of("C1", "C2", "C3", "C4")) {
			seats.add(new Seat(signature, 70000, "S"));
		}
		return seats;
	}

	private void upsertSeatStats(Long layoutId, int seatCount, int minPrice, int maxPrice) {
		entityManager.createNativeQuery("""
			insert into seat_layout_stats (layout_id, seat_count, min_price, max_price, last_updated)
			values (:layoutId, :seatCount, :minPrice, :maxPrice, current_timestamp)
			on duplicate key update
			  seat_count = values(seat_count),
			  min_price = values(min_price),
			  max_price = values(max_price),
			  last_updated = values(last_updated)
			""")
			.setParameter("layoutId", layoutId)
			.setParameter("seatCount", seatCount)
			.setParameter("minPrice", minPrice)
			.setParameter("maxPrice", maxPrice)
			.executeUpdate();
	}

	private record DemoEvent(String title, Long categoryId, RegionLocation regionLocation, String locationName,
		String hallName, String thumbnailUrl, String description) {
	}
}
