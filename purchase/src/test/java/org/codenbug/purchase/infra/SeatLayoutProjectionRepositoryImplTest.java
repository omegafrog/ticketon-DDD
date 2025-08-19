package org.codenbug.purchase.infra;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Set;

import org.codenbug.purchase.query.model.Seat;
import org.codenbug.purchase.query.model.SeatLayoutProjection;
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
public class SeatLayoutProjectionRepositoryImplTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private JpaSeatLayoutProjectionRepository jpaRepository;

    @Test
    void testSaveAndFindSeatLayoutProjection() {
        // Given: SeatLayoutProjection 생성
        Long layoutId = 100L;
        String layout = "테스트 좌석 배치";
        String locationName = "테스트 위치";
        String hallName = "테스트 홀";
        
        Set<Seat> seats = new java.util.HashSet<>(Set.of(
            new Seat("seat-1", "A-01", 50000, "VIP"),
            new Seat("seat-2", "A-02", 50000, "VIP"),
            new Seat("seat-3", "B-01", 30000, "STANDARD")
        ));

        SeatLayoutProjection projection = new SeatLayoutProjection(
            layoutId, layout, locationName, hallName, seats
        );

        // When: 저장
        SeatLayoutProjection savedProjection = jpaRepository.save(projection);
        entityManager.flush();

        // Then: 저장된 데이터 검증
        assertNotNull(savedProjection);
        assertEquals(layoutId, savedProjection.getLayoutId());

        // 조회 테스트
        SeatLayoutProjection foundProjection = jpaRepository.findById(layoutId).orElse(null);
        assertNotNull(foundProjection);
        
        assertAll("저장된 SeatLayoutProjection 검증",
            () -> assertEquals(layoutId, foundProjection.getLayoutId()),
            () -> assertEquals(layout, foundProjection.getLayout()),
            () -> assertEquals(locationName, foundProjection.getLocationName()),
            () -> assertEquals(hallName, foundProjection.getHallName()),
            () -> assertEquals(3, foundProjection.getSeats().size())
        );

        // 좌석 정보 검증
        Set<Seat> foundSeats = foundProjection.getSeats();
        assertTrue(foundSeats.stream().anyMatch(seat -> 
            "seat-1".equals(seat.getSeatId()) && "A-01".equals(seat.getSignature())
        ));
        assertTrue(foundSeats.stream().anyMatch(seat -> 
            "seat-3".equals(seat.getSeatId()) && seat.getAmount() == 30000
        ));
    }

    @Test
    void testExistsByIdMethod() {
        // Given: SeatLayoutProjection 저장
        Long layoutId = 200L;
        SeatLayoutProjection projection = new SeatLayoutProjection(
            layoutId, "Exist Test Layout", "Test Location", "Test Hall", Set.of()
        );
        jpaRepository.save(projection);
        entityManager.flush();

        // When & Then: 존재하는 경우
        assertTrue(jpaRepository.existsById(layoutId), "저장된 레이아웃은 존재해야 합니다");

        // When & Then: 존재하지 않는 경우
        assertFalse(jpaRepository.existsById(999L), "존재하지 않는 레이아웃은 false를 반환해야 합니다");
    }

    @Test
    void testRepositoryImplWrapper() {
        // Given: Repository 구현체 테스트
        SeatLayoutProjectionRepositoryImpl repositoryImpl = new SeatLayoutProjectionRepositoryImpl(jpaRepository);
        
        Long layoutId = 300L;
        Set<Seat> seats = new java.util.HashSet<>(Set.of(
            new Seat("wrapper-seat-1", "W-01", 25000, "ECONOMY")
        ));
        
        SeatLayoutProjection projection = new SeatLayoutProjection(
            layoutId, "Wrapper Test Layout", "Wrapper Location", "Wrapper Hall", seats
        );

        // When: Repository wrapper를 통한 저장
        SeatLayoutProjection savedProjection = repositoryImpl.save(projection);
        entityManager.flush();

        // Then: Repository wrapper를 통한 조회 및 검증
        assertTrue(repositoryImpl.existsById(layoutId));
        SeatLayoutProjection foundProjection = repositoryImpl.findById(layoutId);
        
        assertNotNull(foundProjection);
        assertEquals(layoutId, foundProjection.getLayoutId());
        assertEquals("Wrapper Test Layout", foundProjection.getLayout());
        assertEquals("Wrapper Location", foundProjection.getLocationName());
        assertEquals("Wrapper Hall", foundProjection.getHallName());
        assertEquals(1, foundProjection.getSeats().size());
    }
    
    @Test
    void testUpdateSeatLayoutProjection() {
        // Given: 기존 SeatLayoutProjection 저장
        Long layoutId = 400L;
        Set<Seat> originalSeats = new java.util.HashSet<>(Set.of(
            new Seat("original-seat", "O-01", 10000, "BASIC")
        ));
        
        SeatLayoutProjection projection = new SeatLayoutProjection(
            layoutId, "Original Layout", "Original Location", "Original Hall", originalSeats
        );
        
        jpaRepository.save(projection);
        entityManager.flush();
        entityManager.clear();

        // When: 업데이트
        SeatLayoutProjection foundProjection = jpaRepository.findById(layoutId).orElseThrow();
        Set<Seat> updatedSeats = new java.util.HashSet<>(Set.of(
            new Seat("updated-seat-1", "U-01", 20000, "UPDATED"),
            new Seat("updated-seat-2", "U-02", 20000, "UPDATED")
        ));
        
        foundProjection.updateFrom("Updated Layout", "Updated Location", "Updated Hall", updatedSeats);
        jpaRepository.save(foundProjection);
        entityManager.flush();

        // Then: 업데이트된 데이터 검증
        SeatLayoutProjection updatedProjection = jpaRepository.findById(layoutId).orElseThrow();
        
        assertAll("업데이트된 SeatLayoutProjection 검증",
            () -> assertEquals("Updated Layout", updatedProjection.getLayout()),
            () -> assertEquals("Updated Location", updatedProjection.getLocationName()),
            () -> assertEquals("Updated Hall", updatedProjection.getHallName()),
            () -> assertEquals(2, updatedProjection.getSeats().size())
        );
    }
}