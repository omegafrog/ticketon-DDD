package org.codenbug.messagedispatcher.thread;

import static org.codenbug.messagedispatcher.redis.RedisConfig.*;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.codenbug.messagedispatcher.config.QueueProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;

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
  private final QueueProperties queueProperties;
  private final Counter expiredTokens;
  private final Counter releasedSlots;
  private final Counter duplicateReleases;

  public QueueReaper(StringRedisTemplate redisTemplate, QueueProperties queueProperties, MeterRegistry meterRegistry) {
    this.redisTemplate = redisTemplate;
    this.queueProperties = queueProperties;
    this.expiredTokens = Counter.builder("queue.entry_token.expired.total").register(meterRegistry);
    this.releasedSlots = Counter.builder("queue.slot.released.total").register(meterRegistry);
    this.duplicateReleases = Counter.builder("queue.slot.release.duplicate.total").register(meterRegistry);
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
    Set<String> candidateUserIds = redisTemplate.opsForZSet()
        .rangeByScore(ENTRY_LAST_SEEN_KEY, 0, cutoff, 0, BATCH_LIMIT);

    if (candidateUserIds == null || candidateUserIds.isEmpty()) {
      return;
    }

    int reapedCount = 0;

    for (String userId : candidateUserIds) {
      String entryTokenKey = ENTRY_TOKEN_PREFIX + userId;

      // entryAuthToken이 아직 살아 있으면 ENTRY 상태를 지우지 않는다.
      if (Boolean.TRUE.equals(redisTemplate.hasKey(entryTokenKey))) {
        continue;
      }

      // 여기부터는 entryAuthToken TTL 만료로 토큰은 사라졌는데,
      // ENTRY 상태 관련 키만 남은 경우를 정리한다.
      String eventId = stringValue(redisTemplate.opsForValue().get(ENTRY_EVENT_PREFIX + userId));

      redisTemplate.delete(ENTRY_EVENT_PREFIX + userId);

      if (eventId != null && !eventId.isBlank()) {
        boolean released = releaseSlot(eventId);
        if (released) {
          releasedSlots.increment();
        } else {
          duplicateReleases.increment();
        }
        expiredTokens.increment();
      }

      redisTemplate.opsForZSet().remove(ENTRY_LAST_SEEN_KEY, userId);
      redisTemplate.delete(USER_QUEUE_EVENT_PREFIX + userId);

      reapedCount++;
    }

    if (reapedCount > 0) {
      log.debug("Reaped {} orphan entry users whose entryAuthToken expired", reapedCount);
    }
  }

  private boolean releaseSlot(String eventId) {
    Long released = redisTemplate.execute(new org.springframework.data.redis.core.script.DefaultRedisScript<>("""
        local current = redis.call("HGET", KEYS[1], ARGV[1])
        local max = tonumber(ARGV[2])
        if current == false then
          redis.call("HSET", KEYS[1], ARGV[1], max)
          return 1
        end
        local currentNumber = tonumber(current)
        if currentNumber == nil or currentNumber >= max then
          return 0
        end
        redis.call("HINCRBY", KEYS[1], ARGV[1], 1)
        return 1
        """, Long.class), List.of(ENTRY_QUEUE_SLOTS_KEY_NAME), eventId,
        String.valueOf(queueProperties.getMaxActiveShoppers()));
    return released != null && released > 0;
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
