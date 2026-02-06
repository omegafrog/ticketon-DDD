package org.codenbug.messagedispatcher.thread;

import static org.codenbug.messagedispatcher.redis.RedisConfig.*;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class QueueReaper {

    private static final Logger log = LoggerFactory.getLogger(QueueReaper.class);

    private static final long STALE_WINDOW_MS = 30_000;
    private static final int BATCH_LIMIT = 1000;
    private static final int SCAN_COUNT = 1000;

    private static final String WAITING_LAST_SEEN_PREFIX = "WAITING_LAST_SEEN:";
    private static final String WAITING_QUEUE_RECORD_PREFIX = "WAITING_QUEUE_INDEX_RECORD:";
    private static final String USER_QUEUE_EVENT_PREFIX = "USER_QUEUE_EVENT:";
    private static final String ENTRY_LAST_SEEN_KEY = "ENTRY_LAST_SEEN";
    private static final String ENTRY_TOKEN_PREFIX = "ENTRY_TOKEN:";
    private static final String ENTRY_EVENT_PREFIX = "ENTRY_EVENT:";

    private final StringRedisTemplate redisTemplate;

    public QueueReaper(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Scheduled(fixedRate = 1000)
    public void reapStaleUsers() {
        long cutoff = System.currentTimeMillis() - STALE_WINDOW_MS;
        reapWaitingUsers(cutoff);
        reapEntryUsers(cutoff);
    }

    private void reapWaitingUsers(long cutoff) {
        List<String> lastSeenKeys = scanKeys(WAITING_LAST_SEEN_PREFIX + "*");
        if (lastSeenKeys.isEmpty()) {
            return;
        }

        for (String lastSeenKey : lastSeenKeys) {
            String eventId = extractEventId(lastSeenKey);
            if (eventId == null) {
                log.warn("Skipping unexpected waiting last-seen key: {}", lastSeenKey);
                continue;
            }

            Set<String> staleUserIds = redisTemplate.opsForZSet()
                .rangeByScore(lastSeenKey, 0, cutoff, 0, BATCH_LIMIT);
            if (staleUserIds == null || staleUserIds.isEmpty()) {
                continue;
            }

            String waitingQueueKey = WAITING_QUEUE_KEY_NAME + ":" + eventId;
            String waitingUserIdsKey = WAITING_USER_IDS_KEY_NAME + ":" + eventId;
            String waitingRecordKey = WAITING_QUEUE_RECORD_PREFIX + eventId;

            for (Object rawUserId : staleUserIds) {
                String userId = rawUserId.toString();
                redisTemplate.opsForZSet().remove(waitingQueueKey, rawUserId);
                redisTemplate.opsForHash().delete(waitingUserIdsKey, userId);
                redisTemplate.opsForHash().delete(waitingRecordKey, userId);
                redisTemplate.opsForZSet().remove(lastSeenKey, rawUserId);
                clearUserQueueEventIfMatch(userId, eventId);
            }

            log.debug("Reaped {} stale waiting users for event {}", staleUserIds.size(), eventId);
        }
    }

    private void reapEntryUsers(long cutoff) {
        Set<String> staleUserIds = redisTemplate.opsForZSet()
            .rangeByScore(ENTRY_LAST_SEEN_KEY, 0, cutoff, 0, BATCH_LIMIT);
        if (staleUserIds == null || staleUserIds.isEmpty()) {
            return;
        }

        for (Object rawUserId : staleUserIds) {
            String userId = rawUserId.toString();
            String eventId = stringValue(redisTemplate.opsForValue().get(ENTRY_EVENT_PREFIX + userId));

            redisTemplate.delete(ENTRY_TOKEN_PREFIX + userId);
            redisTemplate.delete(ENTRY_EVENT_PREFIX + userId);
            if (eventId != null && !eventId.isBlank()) {
                redisTemplate.opsForHash().increment(ENTRY_QUEUE_SLOTS_KEY_NAME, eventId, 1);
            }
            redisTemplate.opsForZSet().remove(ENTRY_LAST_SEEN_KEY, rawUserId);
            redisTemplate.delete(USER_QUEUE_EVENT_PREFIX + userId);
        }

        log.debug("Reaped {} stale entry users", staleUserIds.size());
    }

    private void clearUserQueueEventIfMatch(String userId, String eventId) {
        String key = USER_QUEUE_EVENT_PREFIX + userId;
        String recordedEventId = stringValue(redisTemplate.opsForValue().get(key));
        if (eventId.equals(recordedEventId)) {
            redisTemplate.delete(key);
        }
    }

    private String extractEventId(String lastSeenKey) {
        if (!lastSeenKey.startsWith(WAITING_LAST_SEEN_PREFIX)) {
            return null;
        }
        String eventId = lastSeenKey.substring(WAITING_LAST_SEEN_PREFIX.length());
        return eventId.isEmpty() ? null : eventId;
    }

    private List<String> scanKeys(String pattern) {
        List<String> keys = redisTemplate.execute((RedisConnection connection) -> {
            List<String> results = new ArrayList<>();
            try (Cursor<byte[]> cursor = connection.scan(
                ScanOptions.scanOptions().match(pattern).count(SCAN_COUNT).build())) {
                while (cursor.hasNext()) {
                    results.add(new String(cursor.next(), StandardCharsets.UTF_8));
                }
            }
            return results;
        });

        return keys == null ? List.of() : keys;
    }

    private String stringValue(Object value) {
        return value == null ? null : value.toString();
    }
}
