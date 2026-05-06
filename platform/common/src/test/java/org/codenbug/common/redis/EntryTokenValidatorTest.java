package org.codenbug.common.redis;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.codenbug.common.exception.AccessDeniedException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

class EntryTokenValidatorTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    private EntryTokenValidator entryTokenValidator;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        entryTokenValidator = new EntryTokenValidator(redisTemplate);
    }

    @Test
    void 검증_토큰_누락_시_예외() {
        String userId = "u1";
        String token = "t1";

        when(valueOperations.get("ENTRY_TOKEN:" + userId)).thenReturn(null);

        assertThrows(AccessDeniedException.class, () -> entryTokenValidator.validate(userId, token));
    }

    @Test
    void 검증_토큰_불일치_시_예외() {
        String userId = "u1";
        String token = "t1";

        when(valueOperations.get("ENTRY_TOKEN:" + userId)).thenReturn("t2");

        assertThrows(AccessDeniedException.class, () -> entryTokenValidator.validate(userId, token));
    }

    @Test
    void 검증_저장된_토큰_따옴표_제거() {
        String userId = "u1";
        String token = "t1";

        when(valueOperations.get("ENTRY_TOKEN:" + userId)).thenReturn("\"t1\"");

        assertDoesNotThrow(() -> entryTokenValidator.validate(userId, token));
    }

    @Test
    void 이벤트_검증_이벤트_ID_빈값_시_예외() {
        String userId = "u1";
        String token = "t1";

        when(valueOperations.get("ENTRY_TOKEN:" + userId)).thenReturn("t1");

        assertThrows(AccessDeniedException.class, () -> entryTokenValidator.validate(userId, token, ""));
    }

    @Test
    void 이벤트_검증_이벤트_바인딩_없음_시_예외() {
        String userId = "u1";
        String token = "t1";
        String eventId = "e1";

        when(valueOperations.get("ENTRY_TOKEN:" + userId)).thenReturn("t1");
        when(valueOperations.get("ENTRY_EVENT:" + userId)).thenReturn(null);

        assertThrows(AccessDeniedException.class, () -> entryTokenValidator.validate(userId, token, eventId));
    }

    @Test
    void 이벤트_검증_이벤트_불일치_시_예외() {
        String userId = "u1";
        String token = "t1";
        String eventId = "e1";

        when(valueOperations.get("ENTRY_TOKEN:" + userId)).thenReturn("t1");
        when(valueOperations.get("ENTRY_EVENT:" + userId)).thenReturn("e2");

        assertThrows(AccessDeniedException.class, () -> entryTokenValidator.validate(userId, token, eventId));
    }

    @Test
    void 이벤트_검증_토큰_이벤트_일치_허용() {
        String userId = "u1";
        String token = "t1";
        String eventId = "e1";

        when(valueOperations.get("ENTRY_TOKEN:" + userId)).thenReturn("t1");
        when(valueOperations.get("ENTRY_EVENT:" + userId)).thenReturn("e1");

        assertDoesNotThrow(() -> entryTokenValidator.validate(userId, token, eventId));
    }
}
