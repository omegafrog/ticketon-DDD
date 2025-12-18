package org.codenbug.purchase.infra;

import static org.junit.jupiter.api.Assertions.*;

import org.codenbug.purchase.query.model.EventProjection;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.TestPropertySource;

@DataJpaTest
@TestPropertySource(properties = {
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
    "spring.datasource.driver-class-name=org.h2.Driver"
})
public class EventProjectionRepositoryImplTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private JpaPurchaseEventProjectionRepository jpaRepository;

    @Test
    void testSaveAndFindEventProjection() {
        // Given: EventProjection 생성
        String eventId = "test-event-123";
        String title = "테스트 콘서트";
        String managerId = "manager-456";
        Long seatLayoutId = 789L;
        boolean seatSelectable = true;
        String location = "서울 올림픽공원";
        String startTime = "2024-12-25T19:00:00";
        String endTime = "2024-12-25T22:00:00";

        EventProjection projection = new EventProjection(
            eventId, title, managerId, seatLayoutId, seatSelectable, 
            location, startTime, endTime, 1L, "OPEN"
        );

        // When: 저장
        EventProjection savedProjection = jpaRepository.save(projection);
        entityManager.flush();

        // Then: 저장된 데이터 검증
        assertNotNull(savedProjection);
        assertEquals(eventId, savedProjection.getEventId());

        // 조회 테스트
        EventProjection foundProjection = jpaRepository.findById(eventId).orElse(null);
        assertNotNull(foundProjection);
        
        assertAll("저장된 EventProjection 검증",
            () -> assertEquals(eventId, foundProjection.getEventId()),
            () -> assertEquals(title, foundProjection.getTitle()),
            () -> assertEquals(managerId, foundProjection.getManagerId()),
            () -> assertEquals(seatLayoutId, foundProjection.getSeatLayoutId()),
            () -> assertEquals(seatSelectable, foundProjection.isSeatSelectable()),
            () -> assertEquals(location, foundProjection.getLocation()),
            () -> assertEquals(startTime, foundProjection.getStartTime()),
            () -> assertEquals(endTime, foundProjection.getEndTime())
        );
    }

    @Test
    void testExistsByIdMethod() {
        // Given: EventProjection 저장
        String eventId = "exist-test-event";
        EventProjection projection = new EventProjection(
            eventId, "Exist Test", "manager-1", 100L, true, 
            "Test Location", "2024-12-25T19:00:00", "2024-12-25T22:00:00", 1L, "OPEN"
        );
        jpaRepository.save(projection);
        entityManager.flush();

        // When & Then: 존재하는 경우
        assertTrue(jpaRepository.existsById(eventId), "저장된 이벤트는 존재해야 합니다");

        // When & Then: 존재하지 않는 경우
        assertFalse(jpaRepository.existsById("non-existent-event"), "존재하지 않는 이벤트는 false를 반환해야 합니다");
    }

    @Test
    void testRepositoryImplWrapper() {
        // Given: Repository 구현체 테스트
        EventProjectionRepositoryImpl repositoryImpl = new EventProjectionRepositoryImpl(jpaRepository);
        
        String eventId = "wrapper-test-event";
        EventProjection projection = new EventProjection(
            eventId, "Wrapper Test", "manager-wrapper", 200L, false, 
            "Wrapper Location", "2024-12-26T20:00:00", "2024-12-26T23:00:00", 1L, "OPEN"
        );

        // When: Repository wrapper를 통한 저장
        EventProjection savedProjection = repositoryImpl.save(projection);
        entityManager.flush();

        // Then: Repository wrapper를 통한 조회 및 검증
        assertTrue(repositoryImpl.existById(eventId));
        EventProjection foundProjection = repositoryImpl.findByEventId(eventId);
        
        assertNotNull(foundProjection);
        assertEquals(eventId, foundProjection.getEventId());
        assertEquals("Wrapper Test", foundProjection.getTitle());
        assertEquals("manager-wrapper", foundProjection.getManagerId());
    }
}