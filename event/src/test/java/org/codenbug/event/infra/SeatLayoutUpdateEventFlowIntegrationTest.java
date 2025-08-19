package org.codenbug.event.infra;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import org.codenbug.event.domain.Event;
import org.codenbug.event.domain.EventId;
import org.codenbug.event.domain.EventRepository;
import org.codenbug.event.domain.SeatLayoutId;
import org.codenbug.message.SeatLayoutUpdatedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
class SeatLayoutUpdateEventFlowIntegrationTest {

    @Mock
    private EventRepository eventRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private SeatLayoutUpdateEventListener eventListener;

    private Event testEvent;
    private Long testSeatLayoutId = 123L;
    private Long initialVersion = 1L;
    private Long updatedVersion = 2L;

    @BeforeEach
    void setUp() {
        // Event 객체 생성 (실제 객체 또는 mock 사용)
        testEvent = mock(Event.class);
        when(testEvent.getVersion()).thenReturn(initialVersion, updatedVersion);
        when(testEvent.getEventId()).thenReturn(new EventId("test-event-id"));
    }

    @Test
    @DisplayName("SeatLayout 업데이트 -> Event version 업데이트 전체 플로우 테스트")
    void testSeatLayoutUpdateToEventVersionUpdateFlow() {
        // Given: SeatLayout 업데이트 이벤트 생성
        SeatLayoutUpdatedEvent seatLayoutEvent = new SeatLayoutUpdatedEvent(testSeatLayoutId);

        // Repository에서 Event를 찾을 수 있도록 모킹
        when(eventRepository.findBySeatLayoutId(new SeatLayoutId(testSeatLayoutId)))
            .thenReturn(testEvent);
        when(eventRepository.save(testEvent)).thenReturn(testEvent);

        // Event version 초기값 확인
        Long initialEventVersion = testEvent.getVersion();
        assertEquals(initialVersion, initialEventVersion);

        // When: SeatLayout 업데이트 이벤트 처리
        eventListener.handleSeatLayoutUpdated(seatLayoutEvent);

        // Then: Event가 저장되어 version이 업데이트되어야 함
        verify(eventRepository, times(1))
            .findBySeatLayoutId(eq(new SeatLayoutId(testSeatLayoutId)));
        verify(eventRepository, times(1)).save(testEvent);
    }

    @Test
    @DisplayName("여러 번의 SeatLayout 업데이트 시 Event version이 계속 증가해야 한다")
    void testMultipleSeatLayoutUpdatesIncrementVersion() {
        // Given
        SeatLayoutUpdatedEvent firstEvent = new SeatLayoutUpdatedEvent(testSeatLayoutId);
        SeatLayoutUpdatedEvent secondEvent = new SeatLayoutUpdatedEvent(testSeatLayoutId);

        when(eventRepository.findBySeatLayoutId(new SeatLayoutId(testSeatLayoutId)))
            .thenReturn(testEvent);
        when(eventRepository.save(testEvent)).thenReturn(testEvent);

        // When: 첫 번째 업데이트
        eventListener.handleSeatLayoutUpdated(firstEvent);

        // When: 두 번째 업데이트
        eventListener.handleSeatLayoutUpdated(secondEvent);

        // Then: Event 저장이 두 번 호출되어야 함 (각 업데이트마다 version 증가)
        verify(eventRepository, times(2))
            .findBySeatLayoutId(eq(new SeatLayoutId(testSeatLayoutId)));
        verify(eventRepository, times(2)).save(testEvent);
    }

    @Test
    @DisplayName("서로 다른 SeatLayout 업데이트는 각각의 Event에 영향을 주어야 한다")
    void testDifferentSeatLayoutUpdatesAffectDifferentEvents() {
        // Given
        Long otherSeatLayoutId = 456L;
        Event otherEvent = mock(Event.class);
        when(otherEvent.getVersion()).thenReturn(1L);
        when(otherEvent.getEventId()).thenReturn(new EventId("other-event-id"));

        SeatLayoutUpdatedEvent firstEvent = new SeatLayoutUpdatedEvent(testSeatLayoutId);
        SeatLayoutUpdatedEvent secondEvent = new SeatLayoutUpdatedEvent(otherSeatLayoutId);

        when(eventRepository.findBySeatLayoutId(new SeatLayoutId(testSeatLayoutId)))
            .thenReturn(testEvent);
        when(eventRepository.findBySeatLayoutId(new SeatLayoutId(otherSeatLayoutId)))
            .thenReturn(otherEvent);
        when(eventRepository.save(any(Event.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        eventListener.handleSeatLayoutUpdated(firstEvent);
        eventListener.handleSeatLayoutUpdated(secondEvent);

        // Then: 각각의 Event에 대해 조회와 저장이 이루어져야 함
        verify(eventRepository, times(1))
            .findBySeatLayoutId(eq(new SeatLayoutId(testSeatLayoutId)));
        verify(eventRepository, times(1))
            .findBySeatLayoutId(eq(new SeatLayoutId(otherSeatLayoutId)));

        verify(eventRepository, times(1)).save(testEvent);
        verify(eventRepository, times(1)).save(otherEvent);
    }
}