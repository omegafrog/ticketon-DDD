package org.codenbug.seat.app;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.Arrays;
import java.util.List;

import org.codenbug.message.SeatLayoutUpdatedEvent;
import org.codenbug.redislock.RedisLockService;
import org.codenbug.seat.domain.SeatLayout;
import org.codenbug.seat.domain.SeatLayoutRepository;
import org.codenbug.seat.global.RegisterSeatLayoutDto;
import org.codenbug.seat.global.SeatDto;
import org.codenbug.seat.infra.EventServiceClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
class UpdateSeatLayoutServiceTest {

    @Mock
    private SeatLayoutRepository seatLayoutRepository;

    @Mock
    private EventServiceClient eventServiceClient;

    @Mock
    private SeatTransactionService seatTransactionService;

    @Mock
    private RedisLockService redisLockService;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private UpdateSeatLayoutService updateSeatLayoutService;

    private SeatLayout testSeatLayout;
    private RegisterSeatLayoutDto updateRequest;
    private Long testSeatLayoutId = 123L;

    @BeforeEach
    void setUp() {
        // SeatLayout mock 생성
        testSeatLayout = mock(SeatLayout.class);

        // 업데이트 요청 DTO 생성
        List<List<String>> layout =
                Arrays.asList(Arrays.asList("A1", "A2"), Arrays.asList("B1", "B2"));
        List<SeatDto> seatDtos = Arrays.asList(new SeatDto("A001", "A1", "VIP", 100000, true),
                new SeatDto("A002", "A2", "VIP", 100000, true));
        updateRequest =
                new RegisterSeatLayoutDto(layout, seatDtos, "Updated Location", "Updated Hall");
    }

    @Test
    @DisplayName("SeatLayout 업데이트 시 SeatLayoutUpdatedEvent가 발행되어야 한다")
    void testUpdateSeatLayoutPublishesEvent() {
        // Given
        when(seatLayoutRepository.findSeatLayout(testSeatLayoutId)).thenReturn(testSeatLayout);
        when(seatLayoutRepository.save(testSeatLayout)).thenReturn(testSeatLayout);

        // When
        updateSeatLayoutService.update(testSeatLayoutId, updateRequest);

        // Then
        // SeatLayout이 업데이트되었는지 확인
        verify(testSeatLayout).update(any(List.class), anyList());
        verify(seatLayoutRepository).save(testSeatLayout);

        // SeatLayoutUpdatedEvent가 발행되었는지 확인
        ArgumentCaptor<SeatLayoutUpdatedEvent> eventCaptor =
                ArgumentCaptor.forClass(SeatLayoutUpdatedEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());

        SeatLayoutUpdatedEvent publishedEvent = eventCaptor.getValue();
        assertEquals(testSeatLayoutId, publishedEvent.getSeatLayoutId());
    }

    @Test
    @DisplayName("SeatLayout을 찾을 수 없으면 예외가 발생해야 한다")
    void testUpdateSeatLayoutNotFound() {
        // Given
        when(seatLayoutRepository.findSeatLayout(testSeatLayoutId))
                .thenThrow(new RuntimeException("SeatLayout not found"));

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> updateSeatLayoutService.update(testSeatLayoutId, updateRequest));

        assertEquals("SeatLayout not found", exception.getMessage());

        // 이벤트가 발행되지 않았는지 확인
        verify(eventPublisher, never()).publishEvent(any(SeatLayoutUpdatedEvent.class));
        verify(seatLayoutRepository, never()).save(any(SeatLayout.class));
    }

    @Test
    @DisplayName("SeatLayout 업데이트 중 예외 발생 시 이벤트가 발행되지 않아야 한다")
    void testUpdateSeatLayoutWithUpdateException() {
        // Given
        when(seatLayoutRepository.findSeatLayout(testSeatLayoutId)).thenReturn(testSeatLayout);
        doThrow(new RuntimeException("Update failed")).when(testSeatLayout).update(any(List.class),
                anyList());

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> updateSeatLayoutService.update(testSeatLayoutId, updateRequest));

        assertEquals("Update failed", exception.getMessage());

        // 이벤트가 발행되지 않았는지 확인 (트랜잭션 롤백 시나리오)
        verify(eventPublisher, never()).publishEvent(any(SeatLayoutUpdatedEvent.class));
        verify(seatLayoutRepository, never()).save(any(SeatLayout.class));
    }
}
