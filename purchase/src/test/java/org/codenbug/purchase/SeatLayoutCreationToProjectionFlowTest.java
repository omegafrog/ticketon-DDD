package org.codenbug.purchase;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import org.codenbug.message.SeatLayoutCreatedEvent;
import org.codenbug.purchase.domain.SeatLayoutProjectionRepository;
import org.codenbug.purchase.infra.JpaSeatLayoutProjectionRepository;
import org.codenbug.purchase.infra.SeatLayoutProjectionConsumer;
import org.codenbug.purchase.infra.SeatLayoutProjectionRepositoryImpl;
import org.codenbug.purchase.query.model.SeatLayoutProjection;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.TestPropertySource;

/**
 * SeatLayout 생성부터 SeatLayoutProjection 저장까지의 전체 플로우를 테스트합니다.
 * 
 * 플로우:
 * 1. Seat 모듈에서 SeatLayoutCreatedEvent 발행 (시뮬레이션)
 * 2. Purchase 모듈의 SeatLayoutProjectionConsumer가 이벤트 소비
 * 3. SeatLayoutProjection 엔티티 생성 및 DB 저장
 * 4. Repository를 통한 조회 및 검증
 */
@DataJpaTest
@TestPropertySource(properties = {
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
    "spring.datasource.driver-class-name=org.h2.Driver"
})
public class SeatLayoutCreationToProjectionFlowTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private JpaSeatLayoutProjectionRepository jpaRepository;

    @Test
    @DisplayName("SeatLayout 생성 -> Kafka 이벤트 발행 -> SeatLayoutProjection 생성 전체 플로우 테스트")
    void testCompleteSeatLayoutCreationToProjectionFlow() {
        // Given: 컴포넌트 설정 (실제 운영환경에서는 Spring이 자동으로 주입)
        SeatLayoutProjectionRepository seatLayoutProjectionRepository = new SeatLayoutProjectionRepositoryImpl(jpaRepository);
        SeatLayoutProjectionConsumer seatLayoutProjectionConsumer = new SeatLayoutProjectionConsumer(jpaRepository);
        
        // Given: Seat 모듈에서 생성된 좌석 레이아웃 데이터 (실제로는 RegisterSeatLayoutService에서 생성)
        Long layoutId = 1001L;
        String layout = "콘서트홀 A형 배치";
        String locationName = "잠실 롯데콘서트홀";
        String hallName = "메인홀";
        
        List<SeatLayoutCreatedEvent.SeatInfo> seats = List.of(
            new SeatLayoutCreatedEvent.SeatInfo("seat-vip-01", "VIP-A01", 150000, "VIP", true),
            new SeatLayoutCreatedEvent.SeatInfo("seat-vip-02", "VIP-A02", 150000, "VIP", true),
            new SeatLayoutCreatedEvent.SeatInfo("seat-premium-01", "P-B01", 100000, "PREMIUM", true),
            new SeatLayoutCreatedEvent.SeatInfo("seat-premium-02", "P-B02", 100000, "PREMIUM", true),
            new SeatLayoutCreatedEvent.SeatInfo("seat-standard-01", "S-C01", 50000, "STANDARD", true),
            new SeatLayoutCreatedEvent.SeatInfo("seat-standard-02", "S-C02", 50000, "STANDARD", true)
        );
        
        // 1단계: Seat 모듈에서 SeatLayoutCreatedEvent 생성 (실제로는 ApplicationEventPublisher를 통해 발행)
        SeatLayoutCreatedEvent seatLayoutCreatedEvent = new SeatLayoutCreatedEvent(
            layoutId, layout, locationName, hallName, seats
        );
        
        // 2단계: DomainEventPublisher가 Kafka로 이벤트 전송 (실제로는 @TransactionalEventListener에서 처리)
        // 여기서는 직접 Kafka 이벤트 수신을 시뮬레이션
        
        // 3단계: Purchase 모듈의 SeatLayoutProjectionConsumer가 Kafka 이벤트 소비
        // When: 이벤트 소비 및 처리
        seatLayoutProjectionConsumer.handleSeatLayoutCreated(seatLayoutCreatedEvent);
        
        // 4단계: 트랜잭션 커밋 시뮬레이션
        entityManager.flush();
        entityManager.clear();
        
        // Then: SeatLayoutProjection이 올바르게 생성되고 저장되었는지 검증
        
        // Repository를 통한 존재 여부 확인
        assertTrue(seatLayoutProjectionRepository.existsById(layoutId), 
            "SeatLayoutProjection이 데이터베이스에 저장되어야 합니다");
        
        // Repository를 통한 데이터 조회
        SeatLayoutProjection savedProjection = seatLayoutProjectionRepository.findById(layoutId);
        assertNotNull(savedProjection, "저장된 SeatLayoutProjection을 조회할 수 있어야 합니다");
        
        // 저장된 데이터의 정확성 검증
        assertAll("SeatLayout 생성부터 SeatLayoutProjection 저장까지 전체 플로우 검증",
            () -> assertEquals(layoutId, savedProjection.getLayoutId(), 
                "원본 레이아웃 ID와 저장된 ID가 일치해야 합니다"),
            () -> assertEquals(layout, savedProjection.getLayout(), 
                "레이아웃 정보가 정확히 저장되어야 합니다"),
            () -> assertEquals(locationName, savedProjection.getLocationName(), 
                "위치명이 정확히 저장되어야 합니다"),
            () -> assertEquals(hallName, savedProjection.getHallName(), 
                "홀명이 정확히 저장되어야 합니다"),
            () -> assertEquals(6, savedProjection.getSeats().size(), 
                "좌석 수가 정확히 저장되어야 합니다")
        );
        
        // 좌석별 세부 정보 검증
        var savedSeats = savedProjection.getSeats();
        
        // VIP 좌석 검증
        assertTrue(savedSeats.stream().anyMatch(seat -> 
            "seat-vip-01".equals(seat.getSeatId()) && 
            "VIP-A01".equals(seat.getSignature()) && 
            seat.getAmount() == 150000 && 
            "VIP".equals(seat.getGrade())
        ), "VIP 좌석 정보가 정확히 저장되어야 합니다");
        
        // PREMIUM 좌석 검증
        assertTrue(savedSeats.stream().anyMatch(seat -> 
            "seat-premium-01".equals(seat.getSeatId()) && 
            "P-B01".equals(seat.getSignature()) && 
            seat.getAmount() == 100000 && 
            "PREMIUM".equals(seat.getGrade())
        ), "PREMIUM 좌석 정보가 정확히 저장되어야 합니다");
        
        // STANDARD 좌석 검증
        assertTrue(savedSeats.stream().anyMatch(seat -> 
            "seat-standard-01".equals(seat.getSeatId()) && 
            "S-C01".equals(seat.getSignature()) && 
            seat.getAmount() == 50000 && 
            "STANDARD".equals(seat.getGrade())
        ), "STANDARD 좌석 정보가 정확히 저장되어야 합니다");
        
        // 추가 검증: JPA를 통한 직접 조회로도 확인
        SeatLayoutProjection directlyFoundProjection = jpaRepository.findById(layoutId).orElse(null);
        assertNotNull(directlyFoundProjection, "JPA Repository를 통해서도 조회 가능해야 합니다");
        assertEquals(savedProjection.getLayoutId(), directlyFoundProjection.getLayoutId(), 
            "Repository wrapper와 JPA Repository 결과가 일치해야 합니다");
    }
    
    @Test
    @DisplayName("여러 SeatLayout 동시 생성 시나리오 테스트")
    void testMultipleSeatLayoutCreationScenario() {
        // Given: 여러 좌석 레이아웃 동시 생성 시나리오
        SeatLayoutProjectionConsumer consumer = new SeatLayoutProjectionConsumer(jpaRepository);
        
        // 첫 번째 레이아웃: 소규모 카페 공연장
        SeatLayoutCreatedEvent layout1 = new SeatLayoutCreatedEvent(
            100L, "카페 공연장 배치", "홍대 카페거리", "소공연장",
            List.of(
                new SeatLayoutCreatedEvent.SeatInfo("cafe-01", "T-01", 25000, "TABLE", true),
                new SeatLayoutCreatedEvent.SeatInfo("cafe-02", "T-02", 25000, "TABLE", true)
            )
        );
        
        // 두 번째 레이아웃: 대형 야외 무대
        SeatLayoutCreatedEvent layout2 = new SeatLayoutCreatedEvent(
            200L, "야외 페스티벌 무대", "한강공원", "야외무대",
            List.of(
                new SeatLayoutCreatedEvent.SeatInfo("outdoor-vip-01", "V-01", 80000, "VIP", true),
                new SeatLayoutCreatedEvent.SeatInfo("outdoor-general-01", "G-01", 40000, "GENERAL", true),
                new SeatLayoutCreatedEvent.SeatInfo("outdoor-general-02", "G-02", 40000, "GENERAL", true),
                new SeatLayoutCreatedEvent.SeatInfo("outdoor-general-03", "G-03", 40000, "GENERAL", true)
            )
        );
        
        // 세 번째 레이아웃: 클래식 콘서트홀
        SeatLayoutCreatedEvent layout3 = new SeatLayoutCreatedEvent(
            300L, "클래식 콘서트홀 배치", "예술의전당", "콘서트홀",
            List.of(
                new SeatLayoutCreatedEvent.SeatInfo("classic-royal-01", "R-01", 200000, "ROYAL", true),
                new SeatLayoutCreatedEvent.SeatInfo("classic-vip-01", "V-01", 120000, "VIP", true),
                new SeatLayoutCreatedEvent.SeatInfo("classic-premium-01", "P-01", 80000, "PREMIUM", true)
            )
        );
        
        // When: 모든 레이아웃 이벤트 처리
        consumer.handleSeatLayoutCreated(layout1);
        consumer.handleSeatLayoutCreated(layout2);
        consumer.handleSeatLayoutCreated(layout3);
        
        entityManager.flush();
        
        // Then: 모든 SeatLayoutProjection이 저장되었는지 확인
        assertTrue(jpaRepository.existsById(100L), "카페 공연장 레이아웃이 저장되어야 합니다");
        assertTrue(jpaRepository.existsById(200L), "야외 페스티벌 레이아웃이 저장되어야 합니다");
        assertTrue(jpaRepository.existsById(300L), "클래식 콘서트홀 레이아웃이 저장되어야 합니다");
        
        // 각 레이아웃별 데이터 검증
        SeatLayoutProjection cafeLayout = jpaRepository.findById(100L).orElseThrow();
        assertEquals("카페 공연장 배치", cafeLayout.getLayout());
        assertEquals(2, cafeLayout.getSeats().size());
        
        SeatLayoutProjection outdoorLayout = jpaRepository.findById(200L).orElseThrow();
        assertEquals("야외 페스티벌 무대", outdoorLayout.getLayout());
        assertEquals("한강공원", outdoorLayout.getLocationName());
        assertEquals(4, outdoorLayout.getSeats().size());
        
        SeatLayoutProjection classicLayout = jpaRepository.findById(300L).orElseThrow();
        assertEquals("클래식 콘서트홀 배치", classicLayout.getLayout());
        assertEquals("예술의전당", classicLayout.getLocationName());
        assertEquals("콘서트홀", classicLayout.getHallName());
        assertEquals(3, classicLayout.getSeats().size());
        
        // 특정 좌석 정보 검증
        assertTrue(classicLayout.getSeats().stream().anyMatch(seat -> 
            "classic-royal-01".equals(seat.getSeatId()) && seat.getAmount() == 200000
        ), "ROYAL 좌석 정보가 정확해야 합니다");
    }
}