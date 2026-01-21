package org.codenbug.broker.infra;

import static org.codenbug.broker.infra.RedisConfig.*;

import java.util.Map;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class WaitingQueueRedisRepository {

  private final RedisTemplate<String, Object> redisTemplate;

  public WaitingQueueRedisRepository(RedisTemplate<String, Object> redisTemplate) {
    this.redisTemplate = redisTemplate;
  }

  public boolean entryQueueCountExists(String eventId) {
    return Boolean.TRUE
        .equals(redisTemplate.opsForHash().hasKey(ENTRY_QUEUE_SLOTS_KEY_NAME, eventId));
  }

  public void updateEntryQueueCount(String eventId, int seatCount) {
    redisTemplate.opsForHash().put(ENTRY_QUEUE_SLOTS_KEY_NAME, eventId, seatCount);
  }

  public boolean recordWaitingUserIfAbsent(String eventId, String userId) {
    Boolean inserted = redisTemplate.opsForHash()
        .putIfAbsent(WAITING_USER_IDS_KEY_NAME + ":" + eventId, userId, "true");
    return Boolean.TRUE.equals(inserted);
  }

  public long incrementWaitingQueueIdx(String eventId) {
    Long idx =
        redisTemplate.opsForHash().increment(WAITING_QUEUE_IDX_KEY_NAME, eventId.toString(), 1);
    return idx == null ? 0L : idx;
  }

  public void saveUserToWaitingQueue(String userId, String eventId, long idx) {
    redisTemplate.opsForZSet().add(WAITING_QUEUE_KEY_NAME + ":" + eventId,
        Map.of(QUEUE_MESSAGE_USER_ID_KEY_NAME, userId), idx);
  }

  public void saveAdditionalUserData(String userId, String eventId, long idx, String instanceId) {
    redisTemplate.opsForHash().put("WAITING_QUEUE_INDEX_RECORD:" + eventId, userId,
        Map.of(QUEUE_MESSAGE_USER_ID_KEY_NAME, userId, QUEUE_MESSAGE_IDX_KEY_NAME,
            String.valueOf(idx), QUEUE_MESSAGE_EVENT_ID_KEY_NAME, eventId,
            QUEUE_MESSAGE_INSTANCE_ID_KEY_NAME, instanceId));
  }
}
