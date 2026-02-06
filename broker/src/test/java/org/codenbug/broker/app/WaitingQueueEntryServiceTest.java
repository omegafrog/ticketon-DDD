package org.codenbug.broker.app;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.codenbug.broker.config.InstanceConfig;
import org.codenbug.broker.infra.EventStatusInitializer;
import org.codenbug.broker.infra.WaitingQueueRedisRepository;
import org.codenbug.broker.service.SseEmitterService;
import org.codenbug.common.Role;
import org.codenbug.securityaop.aop.LoggedInUserContext;
import org.codenbug.securityaop.aop.UserSecurityToken;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class WaitingQueueEntryServiceTest {

    @Mock
    private SseEmitterService sseEmitterService;

    @Mock
    private WaitingQueueRedisRepository waitingQueueRedisRepository;

    @Mock
    private EventClient eventClient;

    @Mock
    private EventStatusInitializer eventStatusInitializer;

    @Mock
    private InstanceConfig instanceConfig;

    private WaitingQueueEntryService service;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        service = new WaitingQueueEntryService(sseEmitterService, waitingQueueRedisRepository, eventClient,
            eventStatusInitializer, instanceConfig);
    }

    @Test
    void entry_throwsWhenEntryTokenAlreadyIssued() throws Exception {
        String userId = "u1";
        String eventId = "e1";
        when(waitingQueueRedisRepository.isUserExistInEntry(userId)).thenReturn(true);

        try (LoggedInUserContext ignored = LoggedInUserContext
            .open(new UserSecurityToken(userId, "u1@test.local", Role.USER))) {
            IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> service.entry(eventId));
            assertEquals("이미 입장 토큰이 발급되었습니다.", ex.getMessage());
        }

        verify(waitingQueueRedisRepository).isUserExistInEntry(userId);
        verifyNoInteractions(sseEmitterService, eventClient, eventStatusInitializer, instanceConfig);
    }
}
