package org.codenbug.broker.infra;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;

class WaitingQueueRedisRepositoryTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private HashOperations<String, Object, Object> hashOperations;

    private WaitingQueueRedisRepository repository;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        when(redisTemplate.opsForHash()).thenReturn(hashOperations);
        repository = new WaitingQueueRedisRepository(redisTemplate, objectMapper);
    }

    @Test
    void 입장_큐_카운트_이벤트_ID_해시_필드_사용() {
        String eventId = "e1";

        when(hashOperations.hasKey(RedisConfig.ENTRY_QUEUE_SLOTS_KEY_NAME, eventId)).thenReturn(true);
        assertTrue(repository.entryQueueCountExists(eventId));

        when(hashOperations.hasKey(RedisConfig.ENTRY_QUEUE_SLOTS_KEY_NAME, eventId)).thenReturn(false);
        assertFalse(repository.entryQueueCountExists(eventId));
    }

    @Test
    void 입장_큐_카운트_설정_이벤트_ID_해시_필드() {
        String eventId = "e1";
        int slots = 123;

        repository.updateEntryQueueCount(eventId, slots);

        verify(hashOperations).put(RedisConfig.ENTRY_QUEUE_SLOTS_KEY_NAME, eventId, String.valueOf(slots));
    }

    @Test
    void 입장_슬롯_초기화는_좌석수가_아닌_활성_쇼퍼_한도를_사용한다() {
        String eventId = "e1";

        repository.initializeEntryAdmissionSlots(eventId, 500, 10_000);

        verify(hashOperations).putIfAbsent(RedisConfig.ENTRY_QUEUE_SLOTS_KEY_NAME, eventId, "500");
    }
}
