package org.codenbug.purchase.infra;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.codenbug.message.EventCreatedEvent;
import org.codenbug.purchase.query.model.EventProjection;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class EventProjectionConsumerTest {

    @Mock
    private JpaPurchaseEventProjectionRepository eventProjectionRepository;

    private EventProjectionConsumer eventProjectionConsumer;

    @BeforeEach
    void setUp() {
        eventProjectionConsumer = new EventProjectionConsumer(eventProjectionRepository);
    }

    @Test
    void testHandleEventCreated_ShouldCreateAndSaveEventProjection() {
        // Given: EventCreatedEvent 준비
        String eventId = "test-event-123";
        String title = "테스트 콘서트";
        String managerId = "manager-456";
        Long seatLayoutId = 789L;
        boolean seatSelectable = true;
        String location = "서울 올림픽공원";
        String startTime = "2024-12-25T19:00:00";
        String endTime = "2024-12-25T22:00:00";

        EventCreatedEvent event = new EventCreatedEvent(
            eventId, title, managerId, seatLayoutId, seatSelectable, 
            location, startTime, endTime,0, 10000, 1L
        );

        // When: 이벤트 소비 처리
        eventProjectionConsumer.handleEventCreated(event);

        // Then: EventProjection이 올바르게 생성되고 저장되었는지 검증
        ArgumentCaptor<EventProjection> projectionCaptor = ArgumentCaptor.forClass(EventProjection.class);
        verify(eventProjectionRepository, times(1)).save(projectionCaptor.capture());

        EventProjection savedProjection = projectionCaptor.getValue();
        
        assertAll("EventProjection 필드 검증",
            () -> assertEquals(eventId, savedProjection.getEventId(), "이벤트 ID가 일치해야 합니다"),
            () -> assertEquals(title, savedProjection.getTitle(), "제목이 일치해야 합니다"),
            () -> assertEquals(managerId, savedProjection.getManagerId(), "매니저 ID가 일치해야 합니다"),
            () -> assertEquals(seatLayoutId, savedProjection.getSeatLayoutId(), "좌석 레이아웃 ID가 일치해야 합니다"),
            () -> assertEquals(seatSelectable, savedProjection.isSeatSelectable(), "좌석 선택 가능 여부가 일치해야 합니다"),
            () -> assertEquals(location, savedProjection.getLocation(), "장소가 일치해야 합니다"),
            () -> assertEquals(startTime, savedProjection.getStartTime(), "시작 시간이 일치해야 합니다"),
            () -> assertEquals(endTime, savedProjection.getEndTime(), "종료 시간이 일치해야 합니다")
        );
    }

    @Test
    void testHandleEventCreated_WithDifferentData_ShouldCreateCorrectProjection() {
        // Given: 다른 데이터로 EventCreatedEvent 준비
        String eventId = "event-456";
        String title = "뮤지컬 공연";
        String managerId = "manager-789";
        Long seatLayoutId = 100L;
        boolean seatSelectable = false;
        String location = "예술의전당";
        String startTime = "2024-12-31T20:00:00";
        String endTime = "2024-12-31T23:30:00";

        EventCreatedEvent event = new EventCreatedEvent(
            eventId, title, managerId, seatLayoutId, seatSelectable, 
            location, startTime, endTime,0, 10000, 1L
        );

        // When: 이벤트 소비 처리
        eventProjectionConsumer.handleEventCreated(event);

        // Then: EventProjection이 올바르게 생성되고 저장되었는지 검증
        ArgumentCaptor<EventProjection> projectionCaptor = ArgumentCaptor.forClass(EventProjection.class);
        verify(eventProjectionRepository, times(1)).save(projectionCaptor.capture());

        EventProjection savedProjection = projectionCaptor.getValue();
        
        assertAll("다른 데이터로 EventProjection 필드 검증",
            () -> assertEquals(eventId, savedProjection.getEventId()),
            () -> assertEquals(title, savedProjection.getTitle()),
            () -> assertEquals(managerId, savedProjection.getManagerId()),
            () -> assertEquals(seatLayoutId, savedProjection.getSeatLayoutId()),
            () -> assertEquals(seatSelectable, savedProjection.isSeatSelectable()),
            () -> assertEquals(location, savedProjection.getLocation()),
            () -> assertEquals(startTime, savedProjection.getStartTime()),
            () -> assertEquals(endTime, savedProjection.getEndTime())
        );
    }

    @Test
    void testHandleEventCreated_MultipleEvents_ShouldSaveEachProjection() {
        // Given: 여러 개의 EventCreatedEvent 준비
        EventCreatedEvent event1 = new EventCreatedEvent(
            "event-1", "첫 번째 이벤트", "manager-1", 100L, true, 
            "장소1", "2024-12-25T19:00:00", "2024-12-25T22:00:00",0, 10000, 1L
        );
        
        EventCreatedEvent event2 = new EventCreatedEvent(
            "event-2", "두 번째 이벤트", "manager-2", 200L, false, 
            "장소2", "2024-12-26T19:00:00", "2024-12-26T22:00:00",0, 10000, 1L
        );

        // When: 두 이벤트를 순차적으로 처리
        eventProjectionConsumer.handleEventCreated(event1);
        eventProjectionConsumer.handleEventCreated(event2);

        // Then: 두 번의 저장이 호출되었는지 확인
        verify(eventProjectionRepository, times(2)).save(any(EventProjection.class));
        
        ArgumentCaptor<EventProjection> projectionCaptor = ArgumentCaptor.forClass(EventProjection.class);
        verify(eventProjectionRepository, times(2)).save(projectionCaptor.capture());
        
        var savedProjections = projectionCaptor.getAllValues();
        assertEquals(2, savedProjections.size(), "두 개의 EventProjection이 저장되어야 합니다");
        
        // 첫 번째 이벤트 검증
        EventProjection projection1 = savedProjections.get(0);
        assertEquals("event-1", projection1.getEventId());
        assertEquals("첫 번째 이벤트", projection1.getTitle());
        assertTrue(projection1.isSeatSelectable());
        
        // 두 번째 이벤트 검증
        EventProjection projection2 = savedProjections.get(1);
        assertEquals("event-2", projection2.getEventId());
        assertEquals("두 번째 이벤트", projection2.getTitle());
        assertFalse(projection2.isSeatSelectable());
    }
}