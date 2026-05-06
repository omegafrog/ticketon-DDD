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
    void 입장_토큰_이미_발급_시_예외_발생() throws Exception {
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
	void 입장_토큰_발급_시_빠른_폴링_반환() throws Exception {
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
	void 대기_사용자_기본_5초_폴링_사용() throws Exception {
		String userId = "u1";
		String eventId = "e1";
		when(waitingQueueRedisRepository.getEntryToken(userId)).thenReturn(null);
		when(waitingQueueRedisRepository.getUserRank(eventId, userId)).thenReturn(0L);
		when(waitingQueueRedisRepository.getPollingAdaptiveContext(eventId, false)).thenReturn(
			new WaitingQueueRedisRepository.PollingAdaptiveContext("OPEN", 1L, null));

		try (LoggedInUserContext ignored = LoggedInUserContext
			.open(new UserSecurityToken(userId, "u1@test.local", Role.USER))) {
			PollingQueueInfo info = service.parseOrder(eventId);
			assertEquals("WAITING", info.getState());
			assertEquals(1L, info.getRank());
			assertEquals(5000L, info.getPollAfterMs());
		}
	}

	@Test
	void 이벤트_종료_시_느린_폴링() throws Exception {
		String userId = "u1";
		String eventId = "e1";
		when(waitingQueueRedisRepository.getEntryToken(userId)).thenReturn(null);
		when(waitingQueueRedisRepository.getUserRank(eventId, userId)).thenReturn(0L);
		when(waitingQueueRedisRepository.getPollingAdaptiveContext(eventId, false)).thenReturn(
			new WaitingQueueRedisRepository.PollingAdaptiveContext("CLOSED", null, null));

		try (LoggedInUserContext ignored = LoggedInUserContext
			.open(new UserSecurityToken(userId, "u1@test.local", Role.USER))) {
			PollingQueueInfo info = service.parseOrder(eventId);
			assertEquals("WAITING", info.getState());
			assertEquals(1L, info.getRank());
			assertEquals(30000L, info.getPollAfterMs());
		}
	}

	@Test
	void 입장_슬롯_없고_대기_많으면_느린_폴링() throws Exception {
		String userId = "u1";
		String eventId = "e1";
		when(waitingQueueRedisRepository.getEntryToken(userId)).thenReturn(null);
		when(waitingQueueRedisRepository.getUserRank(eventId, userId)).thenReturn(200L);
		when(waitingQueueRedisRepository.getPollingAdaptiveContext(eventId, true)).thenReturn(
			new WaitingQueueRedisRepository.PollingAdaptiveContext("OPEN", 0L, 5000L));

		try (LoggedInUserContext ignored = LoggedInUserContext
			.open(new UserSecurityToken(userId, "u1@test.local", Role.USER))) {
			PollingQueueInfo info = service.parseOrder(eventId);
			assertEquals("WAITING", info.getState());
			assertEquals(201L, info.getRank());
			assertEquals(10000L, info.getPollAfterMs());
		}
	}

	@Test
	void 이벤트_열려_있지_않으면_입장_예외() throws Exception {
		String userId = "u1";
		String eventId = "e1";
		when(waitingQueueRedisRepository.setUserQueueEventIfAbsent(userId, eventId)).thenReturn(true);
		when(waitingQueueRedisRepository.entryQueueCountExists(eventId)).thenReturn(true);
		when(eventClient.getSeatStatus(eventId)).thenReturn("CLOSED");

		try (LoggedInUserContext ignored = LoggedInUserContext
			.open(new UserSecurityToken(userId, "u1@test.local", Role.USER))) {
			IllegalStateException ex = assertThrows(IllegalStateException.class, () -> service.enter(eventId));
			assertEquals("예매 가능한 이벤트가 아닙니다.", ex.getMessage());
		}

		verify(waitingQueueRedisRepository).clearUserQueueEvent(userId);
		verify(waitingQueueRedisRepository, never()).recordWaitingUserIfAbsent(eventId, userId);
	}

	@Test
	void 연결_해제_대기열_없으면_멱등() throws Exception {
		String userId = "u1";
		String eventId = "e1";
		when(waitingQueueRedisRepository.deleteUserFromEntry(userId)).thenReturn(false);
		when(waitingQueueRedisRepository.deleteUserFromWaiting(eventId, userId)).thenReturn(false);

		try (LoggedInUserContext ignored = LoggedInUserContext
			.open(new UserSecurityToken(userId, "u1@test.local", Role.USER))) {
			service.disconnect(eventId);
		}

		verify(waitingQueueRedisRepository).clearUserQueueEvent(userId);
		verify(waitingQueueRedisRepository, never()).incrementEntryQueueCount(eventId);
		verify(waitingQueueRedisRepository, never()).deleteWaitingUserRecord(eventId, userId);
	}

	@Test
	void 연결_해제_입장_토큰_활성일_때만_슬롯_해제() throws Exception {
		String userId = "u1";
		String eventId = "e1";
		when(waitingQueueRedisRepository.deleteUserFromEntry(userId)).thenReturn(true);
		when(waitingQueueRedisRepository.deleteUserFromWaiting(eventId, userId)).thenReturn(false);

		try (LoggedInUserContext ignored = LoggedInUserContext
			.open(new UserSecurityToken(userId, "u1@test.local", Role.USER))) {
			service.disconnect(eventId);
		}

		verify(waitingQueueRedisRepository).incrementEntryQueueCount(eventId);
	}

	@Test
	void 연결_해제_대기_기록_삭제_SLOT_증가_없음() throws Exception {
		String userId = "u1";
		String eventId = "e1";
		when(waitingQueueRedisRepository.deleteUserFromEntry(userId)).thenReturn(false);
		when(waitingQueueRedisRepository.deleteUserFromWaiting(eventId, userId)).thenReturn(true);

		try (LoggedInUserContext ignored = LoggedInUserContext
			.open(new UserSecurityToken(userId, "u1@test.local", Role.USER))) {
			service.disconnect(eventId);
		}

		verify(waitingQueueRedisRepository).deleteWaitingUserRecord(eventId, userId);
		verify(waitingQueueRedisRepository, never()).incrementEntryQueueCount(eventId);
	}
}
