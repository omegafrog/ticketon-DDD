package org.codenbug.purchase;

import static org.junit.jupiter.api.Assertions.*;

import org.codenbug.message.EventCreatedEvent;
import org.codenbug.purchase.domain.EventProjectionRepository;
import org.codenbug.purchase.infra.EventProjectionConsumer;
import org.codenbug.purchase.infra.EventProjectionRepositoryImpl;
import org.codenbug.purchase.infra.JpaPurchaseEventProjectionRepository;
import org.codenbug.purchase.query.model.EventProjection;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.TestPropertySource;

/**
 * 이벤트 생성부터 EventProjection 저장까지의 전체 플로우를 테스트합니다.
 *
 * 플로우:
 * 1. Event 모듈에서 EventCreatedEvent 발행 (시뮬레이션)
 * 2. Purchase 모듈의 EventProjectionConsumer가 이벤트 소비
 * 3. EventProjection 엔티티 생성 및 DB 저장
 * 4. Repository를 통한 조회 및 검증
 */
@DataJpaTest
@TestPropertySource(properties = {
	"spring.jpa.hibernate.ddl-auto=create-drop",
	"spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
	"spring.datasource.driver-class-name=org.h2.Driver"
})
public class EventCreationToProjectionFlowTest {

	@Autowired
	private TestEntityManager entityManager;

	@Autowired
	private JpaPurchaseEventProjectionRepository jpaRepository;

	@Test
	@DisplayName("이벤트 생성 -> Kafka 이벤트 발행 -> EventProjection 생성 전체 플로우 테스트")
	void testCompleteEventCreationToProjectionFlow() {
		// Given: 컴포넌트 설정 (실제 운영환경에서는 Spring이 자동으로 주입)
		EventProjectionRepository eventProjectionRepository = new EventProjectionRepositoryImpl(jpaRepository);
		EventProjectionConsumer eventProjectionConsumer = new EventProjectionConsumer(jpaRepository);

		// Given: Event 모듈에서 생성된 이벤트 데이터 (실제로는 RegisterEventService에서 생성)
		String eventId = "flow-test-event-001";
		String title = "스프링 콘서트 2024";
		String managerId = "manager-spring-001";
		Long seatLayoutId = 1001L;
		boolean seatSelectable = true;
		String location = "코엑스 컨벤션센터";
		String startTime = "2024-12-31T19:00:00";
		String endTime = "2024-12-31T22:30:00";

		// 1단계: Event 모듈에서 EventCreatedEvent 생성 (실제로는 ApplicationEventPublisher를 통해 발행)
		EventCreatedEvent eventCreatedEvent = new EventCreatedEvent(
			eventId, title, managerId, seatLayoutId, seatSelectable,
			location, startTime, endTime, 0, 10000, 1L
		);

		// 2단계: DomainEventPublisher가 Kafka로 이벤트 전송 (실제로는 @TransactionalEventListener에서 처리)
		// 여기서는 직접 Kafka 이벤트 수신되었다고 치고 consumer로 전달

		// 3단계: Purchase 모듈의 EventProjectionConsumer가 Kafka 이벤트 소비
		// When: 이벤트 소비 및 처리
		eventProjectionConsumer.handleEventCreated(eventCreatedEvent);

		// 4단계: 트랜잭션 커밋 시뮬레이션
		entityManager.flush();
		entityManager.clear();

		// Then: EventProjection이 올바르게 생성되고 저장되었는지 검증

		// Repository를 통한 존재 여부 확인
		assertTrue(eventProjectionRepository.existById(eventId),
			"EventProjection이 데이터베이스에 저장되어야 합니다");

		// Repository를 통한 데이터 조회
		EventProjection savedProjection = eventProjectionRepository.findByEventId(eventId);
		assertNotNull(savedProjection, "저장된 EventProjection을 조회할 수 있어야 합니다");

		// 저장된 데이터의 정확성 검증
		assertAll("이벤트 생성부터 EventProjection 저장까지 전체 플로우 검증",
			() -> assertEquals(eventId, savedProjection.getEventId(),
				"원본 이벤트 ID와 저장된 ID가 일치해야 합니다"),
			() -> assertEquals(title, savedProjection.getTitle(),
				"이벤트 제목이 정확히 저장되어야 합니다"),
			() -> assertEquals(managerId, savedProjection.getManagerId(),
				"매니저 ID가 정확히 저장되어야 합니다"),
			() -> assertEquals(seatLayoutId, savedProjection.getSeatLayoutId(),
				"좌석 레이아웃 ID가 정확히 저장되어야 합니다"),
			() -> assertEquals(seatSelectable, savedProjection.isSeatSelectable(),
				"좌석 선택 가능 여부가 정확히 저장되어야 합니다"),
			() -> assertEquals(location, savedProjection.getLocation(),
				"이벤트 장소 정보가 정확히 저장되어야 합니다"),
			() -> assertEquals(startTime, savedProjection.getStartTime(),
				"이벤트 시작 시간이 정확히 저장되어야 합니다"),
			() -> assertEquals(endTime, savedProjection.getEndTime(),
				"이벤트 종료 시간이 정확히 저장되어야 합니다")
		);

		// 추가 검증: JPA를 통한 직접 조회로도 확인
		EventProjection directlyFoundProjection = jpaRepository.findById(eventId).orElse(null);
		assertNotNull(directlyFoundProjection, "JPA Repository를 통해서도 조회 가능해야 합니다");
		assertEquals(savedProjection.getEventId(), directlyFoundProjection.getEventId(),
			"Repository wrapper와 JPA Repository 결과가 일치해야 합니다");
	}

	@Test
	@DisplayName("여러 이벤트 동시 생성 시나리오 테스트")
	void testMultipleEventCreationScenario() {
		// Given: 여러 이벤트 동시 생성 시나리오
		EventProjectionConsumer consumer = new EventProjectionConsumer(jpaRepository);

		// 첫 번째 이벤트
		EventCreatedEvent event1 = new EventCreatedEvent(
			"multi-event-001", "봄 페스티벌", "manager-001", 100L, true,
			"한강공원", "2024-04-15T18:00:00", "2024-04-15T23:00:00",0, 10000, 1L
		);

		// 두 번째 이벤트
		EventCreatedEvent event2 = new EventCreatedEvent(
			"multi-event-002", "여름 콘서트", "manager-002", 200L, false,
			"올림픽공원", "2024-07-20T19:30:00", "2024-07-20T22:30:00",0, 10000, 1L
		);

		// 세 번째 이벤트
		EventCreatedEvent event3 = new EventCreatedEvent(
			"multi-event-003", "가을 뮤지컬", "manager-003", 300L, true,
			"예술의전당", "2024-10-10T14:00:00", "2024-10-10T17:00:00",0, 10000, 1L
		);

		// When: 모든 이벤트 처리
		consumer.handleEventCreated(event1);
		consumer.handleEventCreated(event2);
		consumer.handleEventCreated(event3);

		entityManager.flush();

		// Then: 모든 EventProjection이 저장되었는지 확인
		assertTrue(jpaRepository.existsById("multi-event-001"), "첫 번째 이벤트가 저장되어야 합니다");
		assertTrue(jpaRepository.existsById("multi-event-002"), "두 번째 이벤트가 저장되어야 합니다");
		assertTrue(jpaRepository.existsById("multi-event-003"), "세 번째 이벤트가 저장되어야 합니다");

		// 각 이벤트별 데이터 검증
		EventProjection projection1 = jpaRepository.findById("multi-event-001").orElseThrow();
		assertEquals("봄 페스티벌", projection1.getTitle());
		assertTrue(projection1.isSeatSelectable());

		EventProjection projection2 = jpaRepository.findById("multi-event-002").orElseThrow();
		assertEquals("여름 콘서트", projection2.getTitle());
		assertFalse(projection2.isSeatSelectable());

		EventProjection projection3 = jpaRepository.findById("multi-event-003").orElseThrow();
		assertEquals("가을 뮤지컬", projection3.getTitle());
		assertEquals("예술의전당", projection3.getLocation());
	}
}