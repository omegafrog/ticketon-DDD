package org.codenbug.broker.app;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.codenbug.broker.config.InstanceConfig;
import org.codenbug.broker.infra.EventStatusInitializer;
import org.codenbug.broker.infra.WaitingQueueRedisRepository;
import org.codenbug.broker.ui.PollingQueueInfo;
import org.codenbug.common.Role;
import org.codenbug.securityaop.aop.LoggedInUserContext;
import org.codenbug.securityaop.aop.UserSecurityToken;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class PollingWaitingQueueServiceTest {

    @Mock
    private EventClient eventClient;

    @Mock
    private WaitingQueueRedisRepository waitingQueueRedisRepository;

    @Mock
    private EventStatusInitializer eventStatusInitializer;

    @Mock
    private InstanceConfig instanceConfig;

    private PollingWaitingQueueService service;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        service = new PollingWaitingQueueService(eventClient, waitingQueueRedisRepository,
            eventStatusInitializer, instanceConfig);
    }

    @Test
    void enter_throwsWhenEntryTokenAlreadyIssued() throws Exception {
        String userId = "u1";
        String eventId = "e1";
        when(waitingQueueRedisRepository.isUserExistInEntry(userId)).thenReturn(true);

        try (LoggedInUserContext ignored = LoggedInUserContext
            .open(new UserSecurityToken(userId, "u1@test.local", Role.USER))) {
            IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> service.enter(eventId));
            assertEquals("이미 입장 토큰이 발급되었습니다.", ex.getMessage());
        }

        verify(waitingQueueRedisRepository).isUserExistInEntry(userId);
        verify(waitingQueueRedisRepository, never()).setUserQueueEventIfAbsent(anyString(), anyString());
        verifyNoInteractions(eventClient, eventStatusInitializer, instanceConfig);
    }

	@Test
	void parseOrder_entryTokenIssued_returnsEntryWithFastPolling() throws Exception {
		String userId = "u1";
		String eventId = "e1";
		when(waitingQueueRedisRepository.getEntryToken(userId)).thenReturn("t1");

		try (LoggedInUserContext ignored = LoggedInUserContext
			.open(new UserSecurityToken(userId, "u1@test.local", Role.USER))) {
			PollingQueueInfo info = service.parseOrder(eventId);
			assertEquals("ENTRY", info.getState());
			assertNull(info.getRank());
			assertEquals("t1", info.getEntryAuthToken());
			assertEquals(1000L, info.getPollAfterMs());
		}
	}

	@Test
	void parseOrder_eventClosed_slowsPolling() throws Exception {
		String userId = "u1";
		String eventId = "e1";
		when(waitingQueueRedisRepository.getEntryToken(userId)).thenReturn(null);
		when(waitingQueueRedisRepository.isUserExistInWaiting(eventId, userId)).thenReturn(true);
		when(waitingQueueRedisRepository.getUserRank(eventId, userId)).thenReturn(0L);
		when(waitingQueueRedisRepository.getEventStatus(eventId)).thenReturn("CLOSED");

		try (LoggedInUserContext ignored = LoggedInUserContext
			.open(new UserSecurityToken(userId, "u1@test.local", Role.USER))) {
			PollingQueueInfo info = service.parseOrder(eventId);
			assertEquals("WAITING", info.getState());
			assertEquals(1L, info.getRank());
			assertEquals(30000L, info.getPollAfterMs());
		}
	}

	@Test
	void parseOrder_noEntrySlots_andLargeQueue_slowsPolling() throws Exception {
		String userId = "u1";
		String eventId = "e1";
		when(waitingQueueRedisRepository.getEntryToken(userId)).thenReturn(null);
		when(waitingQueueRedisRepository.isUserExistInWaiting(eventId, userId)).thenReturn(true);
		when(waitingQueueRedisRepository.getUserRank(eventId, userId)).thenReturn(200L);
		when(waitingQueueRedisRepository.getEventStatus(eventId)).thenReturn("OPEN");
		when(waitingQueueRedisRepository.getEntryQueueSlots(eventId)).thenReturn(0L);
		when(waitingQueueRedisRepository.getWaitingQueueSize(eventId)).thenReturn(5000L);

		try (LoggedInUserContext ignored = LoggedInUserContext
			.open(new UserSecurityToken(userId, "u1@test.local", Role.USER))) {
			PollingQueueInfo info = service.parseOrder(eventId);
			assertEquals("WAITING", info.getState());
			assertEquals(201L, info.getRank());
			assertEquals(10000L, info.getPollAfterMs());
		}
	}
}
