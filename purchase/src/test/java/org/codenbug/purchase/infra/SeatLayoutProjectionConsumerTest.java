package org.codenbug.purchase.infra;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Set;

import org.codenbug.message.SeatLayoutCreatedEvent;
import org.codenbug.purchase.query.model.Seat;
import org.codenbug.purchase.query.model.SeatLayoutProjection;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class SeatLayoutProjectionConsumerTest {

    @Mock
    private JpaSeatLayoutProjectionRepository seatLayoutProjectionRepository;

    private SeatLayoutProjectionConsumer seatLayoutProjectionConsumer;

    @BeforeEach
    void setUp() {
        seatLayoutProjectionConsumer =
                new SeatLayoutProjectionConsumer(seatLayoutProjectionRepository);
    }

    @Test
    void testHandleSeatLayoutCreated_ShouldCreateAndSaveSeatLayoutProjection() {
        // Given: SeatLayoutCreatedEvent 준비
        Long layoutId = 123L;
        String layout = "A형 좌석 배치";
        String locationName = "서울 올림픽공원";
        String hallName = "올림픽홀";

        List<SeatLayoutCreatedEvent.SeatInfo> seats = List.of(
                new SeatLayoutCreatedEvent.SeatInfo("seat-1", "A-01", 50000, "VIP", true),
                new SeatLayoutCreatedEvent.SeatInfo("seat-2", "A-02", 50000, "VIP", true),
                new SeatLayoutCreatedEvent.SeatInfo("seat-3", "B-01", 30000, "STANDARD", true));

        SeatLayoutCreatedEvent event =
                new SeatLayoutCreatedEvent(layoutId, layout, locationName, hallName, seats);

        // When: 이벤트 소비 처리
        seatLayoutProjectionConsumer.handleSeatLayoutCreated(event);

        // Then: SeatLayoutProjection이 올바르게 생성되고 저장되었는지 검증
        ArgumentCaptor<SeatLayoutProjection> projectionCaptor =
                ArgumentCaptor.forClass(SeatLayoutProjection.class);
        verify(seatLayoutProjectionRepository, times(1)).save(projectionCaptor.capture());

        SeatLayoutProjection savedProjection = projectionCaptor.getValue();

        assertAll("SeatLayoutProjection 필드 검증",
                () -> assertEquals(layoutId, savedProjection.getLayoutId(), "레이아웃 ID가 일치해야 합니다"),
                () -> assertEquals(layout, savedProjection.getLayout(), "레이아웃 정보가 일치해야 합니다"),
                () -> assertEquals(locationName, savedProjection.getLocationName(),
                        "위치명이 일치해야 합니다"),
                () -> assertEquals(hallName, savedProjection.getHallName(), "홀명이 일치해야 합니다"),
                () -> assertEquals(3, savedProjection.getSeats().size(), "좌석 수가 일치해야 합니다"));

        // Seat 정보 검증
        Set<Seat> savedSeats = savedProjection.getSeats();
        assertTrue(savedSeats.stream()
                .anyMatch(seat -> "seat-1".equals(seat.getSeatId())
                        && "A-01".equals(seat.getSignature()) && seat.getAmount() == 50000
                        && "VIP".equals(seat.getGrade())),
                "VIP 좌석 정보가 정확해야 합니다");

        assertTrue(savedSeats.stream()
                .anyMatch(seat -> "seat-3".equals(seat.getSeatId())
                        && "B-01".equals(seat.getSignature()) && seat.getAmount() == 30000
                        && "STANDARD".equals(seat.getGrade())),
                "STANDARD 좌석 정보가 정확해야 합니다");
    }

    @Test
    void testHandleSeatLayoutCreated_WithDifferentData_ShouldCreateCorrectProjection() {
        // Given: 다른 데이터로 SeatLayoutCreatedEvent 준비
        Long layoutId = 456L;
        String layout = "B형 좌석 배치";
        String locationName = "예술의전당";
        String hallName = "콘서트홀";

        List<SeatLayoutCreatedEvent.SeatInfo> seats = List.of(
                new SeatLayoutCreatedEvent.SeatInfo("seat-100", "C-01", 40000, "PREMIUM", true),
                new SeatLayoutCreatedEvent.SeatInfo("seat-101", "C-02", 40000, "PREMIUM", true));

        SeatLayoutCreatedEvent event =
                new SeatLayoutCreatedEvent(layoutId, layout, locationName, hallName, seats);

        // When: 이벤트 소비 처리
        seatLayoutProjectionConsumer.handleSeatLayoutCreated(event);

        // Then: SeatLayoutProjection이 올바르게 생성되고 저장되었는지 검증
        ArgumentCaptor<SeatLayoutProjection> projectionCaptor =
                ArgumentCaptor.forClass(SeatLayoutProjection.class);
        verify(seatLayoutProjectionRepository, times(1)).save(projectionCaptor.capture());

        SeatLayoutProjection savedProjection = projectionCaptor.getValue();

        assertAll("다른 데이터로 SeatLayoutProjection 필드 검증",
                () -> assertEquals(layoutId, savedProjection.getLayoutId()),
                () -> assertEquals(layout, savedProjection.getLayout()),
                () -> assertEquals(locationName, savedProjection.getLocationName()),
                () -> assertEquals(hallName, savedProjection.getHallName()),
                () -> assertEquals(2, savedProjection.getSeats().size()));
    }

    @Test
    void testHandleSeatLayoutCreated_EmptySeats_ShouldCreateProjectionWithEmptySeats() {
        // Given: 좌석이 없는 SeatLayoutCreatedEvent
        Long layoutId = 789L;
        String layout = "빈 좌석 배치";
        String locationName = "테스트 장소";
        String hallName = "테스트홀";

        List<SeatLayoutCreatedEvent.SeatInfo> seats = List.of(); // 빈 좌석 리스트

        SeatLayoutCreatedEvent event =
                new SeatLayoutCreatedEvent(layoutId, layout, locationName, hallName, seats);

        // When: 이벤트 소비 처리
        seatLayoutProjectionConsumer.handleSeatLayoutCreated(event);

        // Then: SeatLayoutProjection이 생성되고 좌석이 비어있어야 함
        ArgumentCaptor<SeatLayoutProjection> projectionCaptor =
                ArgumentCaptor.forClass(SeatLayoutProjection.class);
        verify(seatLayoutProjectionRepository, times(1)).save(projectionCaptor.capture());

        SeatLayoutProjection savedProjection = projectionCaptor.getValue();

        assertEquals(layoutId, savedProjection.getLayoutId());
        assertEquals(0, savedProjection.getSeats().size(), "좌석이 비어있어야 합니다");
    }
}
