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
    void validate_throwsWhenTokenMissing() {
        String userId = "u1";
        String token = "t1";

        when(valueOperations.get("ENTRY_TOKEN:" + userId)).thenReturn(null);

        assertThrows(AccessDeniedException.class, () -> entryTokenValidator.validate(userId, token));
    }

    @Test
    void validate_throwsWhenTokenMismatch() {
        String userId = "u1";
        String token = "t1";

        when(valueOperations.get("ENTRY_TOKEN:" + userId)).thenReturn("t2");

        assertThrows(AccessDeniedException.class, () -> entryTokenValidator.validate(userId, token));
    }

    @Test
    void validate_stripsQuotesFromStoredToken() {
        String userId = "u1";
        String token = "t1";

        when(valueOperations.get("ENTRY_TOKEN:" + userId)).thenReturn("\"t1\"");

        assertDoesNotThrow(() -> entryTokenValidator.validate(userId, token));
    }

    @Test
    void validateWithEvent_throwsWhenEventIdBlank() {
        String userId = "u1";
        String token = "t1";

        when(valueOperations.get("ENTRY_TOKEN:" + userId)).thenReturn("t1");

        assertThrows(AccessDeniedException.class, () -> entryTokenValidator.validate(userId, token, ""));
    }

    @Test
    void validateWithEvent_throwsWhenEventBindingMissing() {
        String userId = "u1";
        String token = "t1";
        String eventId = "e1";

        when(valueOperations.get("ENTRY_TOKEN:" + userId)).thenReturn("t1");
        when(valueOperations.get("ENTRY_EVENT:" + userId)).thenReturn(null);

        assertThrows(AccessDeniedException.class, () -> entryTokenValidator.validate(userId, token, eventId));
    }

    @Test
    void validateWithEvent_throwsWhenEventMismatch() {
        String userId = "u1";
        String token = "t1";
        String eventId = "e1";

        when(valueOperations.get("ENTRY_TOKEN:" + userId)).thenReturn("t1");
        when(valueOperations.get("ENTRY_EVENT:" + userId)).thenReturn("e2");

        assertThrows(AccessDeniedException.class, () -> entryTokenValidator.validate(userId, token, eventId));
    }

    @Test
    void validateWithEvent_acceptsWhenTokenAndEventMatch() {
        String userId = "u1";
        String token = "t1";
        String eventId = "e1";

        when(valueOperations.get("ENTRY_TOKEN:" + userId)).thenReturn("t1");
        when(valueOperations.get("ENTRY_EVENT:" + userId)).thenReturn("e1");

        assertDoesNotThrow(() -> entryTokenValidator.validate(userId, token, eventId));
    }
}
