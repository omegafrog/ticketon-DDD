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

@ExtendWith(MockitoExtension.class)
class SeatLayoutUpdateEventListenerTest {

    @Mock
    private EventRepository eventRepository;

    @InjectMocks
    private SeatLayoutUpdateEventListener eventListener;

    private Event testEvent;
    private Long testSeatLayoutId = 123L;

    @BeforeEach
    void setUp() {
        // Event mock 생성
        testEvent = mock(Event.class);
    }

    @Test
    @DisplayName("SeatLayout 업데이트 시 연관된 Event의 version이 업데이트되어야 한다")
    void testSeatLayoutUpdatedEventHandling() {
        // Given
        SeatLayoutUpdatedEvent updateEvent = new SeatLayoutUpdatedEvent(testSeatLayoutId);
        
        EventId eventId = new EventId("test-event-id");
        when(testEvent.getEventId()).thenReturn(eventId);
        when(testEvent.getVersion()).thenReturn(1L);
        when(eventRepository.findBySeatLayoutId(any(SeatLayoutId.class))).thenReturn(testEvent);
        when(eventRepository.save(testEvent)).thenReturn(testEvent);

        // When
        eventListener.handleSeatLayoutUpdated(updateEvent);

        // Then
        verify(eventRepository).findBySeatLayoutId(any(SeatLayoutId.class));
        verify(eventRepository).save(testEvent);
        
        // SeatLayoutId로 Event를 조회했는지 확인
        verify(eventRepository, times(1)).findBySeatLayoutId(any(SeatLayoutId.class));
        // Event가 저장되어 version이 업데이트되었는지 확인
        verify(eventRepository, times(1)).save(testEvent);
    }

    @Test
    @DisplayName("해당하는 Event가 없으면 warning 로그만 출력하고 정상 종료되어야 한다")
    void testSeatLayoutUpdatedEventWithNoEvent() {
        // Given
        SeatLayoutUpdatedEvent updateEvent = new SeatLayoutUpdatedEvent(testSeatLayoutId);
        
        when(eventRepository.findBySeatLayoutId(any(SeatLayoutId.class))).thenReturn(null);

        // When & Then
        assertDoesNotThrow(() -> eventListener.handleSeatLayoutUpdated(updateEvent));
        
        verify(eventRepository).findBySeatLayoutId(any(SeatLayoutId.class));
        // Event가 없으므로 save는 호출되지 않아야 함
        verify(eventRepository, never()).save(any(Event.class));
    }

    @Test
    @DisplayName("EventRepository에서 예외 발생 시 RuntimeException이 발생해야 한다")
    void testSeatLayoutUpdatedEventWithRepositoryException() {
        // Given
        SeatLayoutUpdatedEvent updateEvent = new SeatLayoutUpdatedEvent(testSeatLayoutId);
        
        when(eventRepository.findBySeatLayoutId(any(SeatLayoutId.class)))
            .thenThrow(new RuntimeException("Repository 오류"));

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, 
            () -> eventListener.handleSeatLayoutUpdated(updateEvent));
        
        assertTrue(exception.getMessage().contains("SeatLayout 업데이트 이벤트 처리 실패"));
        
        verify(eventRepository).findBySeatLayoutId(any(SeatLayoutId.class));
        // 예외 발생으로 save는 호출되지 않아야 함
        verify(eventRepository, never()).save(any(Event.class));
    }
}