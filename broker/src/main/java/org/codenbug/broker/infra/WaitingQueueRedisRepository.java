package org.codenbug.broker.infra;

import static org.codenbug.broker.infra.RedisConfig.*;

import java.util.List;
import java.util.Map;

import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class WaitingQueueRedisRepository {

	public record PollingAdaptiveContext(String eventStatus, Long entryQueueSlots, Long waitingQueueSize) {
	}

  private final StringRedisTemplate redisTemplate;
	private final ObjectMapper objectMapper;

	public WaitingQueueRedisRepository(StringRedisTemplate redisTemplate, ObjectMapper objectMapper) {
    	this.redisTemplate = redisTemplate;
		this.objectMapper = objectMapper;
	}

  public boolean entryQueueCountExists(String eventId) {
    Boolean exists = redisTemplate.opsForHash().hasKey(ENTRY_QUEUE_SLOTS_KEY_NAME, eventId);
    return Boolean.TRUE.equals(exists);
  }

  public void updateEntryQueueCount(String eventId, int slotCount) {
	  redisTemplate.opsForHash().put(ENTRY_QUEUE_SLOTS_KEY_NAME, eventId, String.valueOf(slotCount));
  }
  public void incrementEntryQueueCount(String eventId) {
    redisTemplate.opsForHash().increment(ENTRY_QUEUE_SLOTS_KEY_NAME, eventId, 1);
  }

  public boolean recordWaitingUserIfAbsent(String eventId, String userId) {
	  return redisTemplate.opsForHash()
		  .putIfAbsent(WAITING_USER_IDS_KEY_NAME + ":" + eventId, userId, "true");
  }

  public long incrementWaitingQueueIdx(String eventId) {
    Long idx =
        redisTemplate.opsForHash().increment(WAITING_QUEUE_IDX_KEY_NAME, eventId.toString(), 1);
    return idx == null ? 0L : idx;
  }

  public void saveUserToWaitingQueue(String userId, String eventId, long idx) {
		try {
			redisTemplate.opsForZSet().add(WAITING_QUEUE_KEY_NAME + ":" + eventId, userId, idx);
		}catch (Exception e) {
			throw new RuntimeException(e);
		}
  }

  public void saveAdditionalUserData(String userId, String eventId, long idx, String instanceId){
		try {
			redisTemplate.opsForHash().put("WAITING_QUEUE_INDEX_RECORD:" + eventId, userId,
				objectMapper.writeValueAsString(
					Map.of(QUEUE_MESSAGE_USER_ID_KEY_NAME, userId, QUEUE_MESSAGE_IDX_KEY_NAME,
						String.valueOf(idx), QUEUE_MESSAGE_EVENT_ID_KEY_NAME, eventId,
						QUEUE_MESSAGE_INSTANCE_ID_KEY_NAME, instanceId)));
		}catch (JsonProcessingException e) {
			throw new RuntimeException(e);
		}
  }

	public Long getUserRank(String eventId, String userId) {
		return redisTemplate.opsForZSet().rank(WAITING_QUEUE_KEY_NAME + ":" + eventId, userId);
	}

	public void deleteUserFromEntry(String userId) {
		redisTemplate.delete(ENTRY_TOKEN_STORAGE_KEY_NAME + ":" + userId);
	}

	public void deleteUserFromWaiting(String eventId, String userId) {
		redisTemplate.opsForZSet().remove(WAITING_QUEUE_KEY_NAME + ":" + eventId, userId);
	}

	public boolean isUserExistInWaiting(String eventId, String userId) {
		return redisTemplate.opsForZSet().score(WAITING_QUEUE_KEY_NAME + ":" + eventId, userId) != null;
	}

	public void updateWaitingLastSeen(String eventId, String userId, long epochMillis) {
		redisTemplate.opsForZSet().add(WAITING_LAST_SEEN_KEY_NAME + ":" + eventId, userId, epochMillis);
	}

	public boolean setUserQueueEventIfAbsent(String userId, String eventId) {
		Boolean inserted = redisTemplate.opsForValue()
			.setIfAbsent(USER_QUEUE_EVENT_KEY_NAME + ":" + userId, eventId);
		return Boolean.TRUE.equals(inserted);
	}

	public String getUserQueueEvent(String userId) {
		Object value = redisTemplate.opsForValue().get(USER_QUEUE_EVENT_KEY_NAME + ":" + userId);
		return value == null ? null : value.toString();
	}

	public boolean isUserExistInEntry(String userId) {
		return redisTemplate.opsForValue().get(ENTRY_TOKEN_STORAGE_KEY_NAME + ":" + userId) != null;
	}

	public String getEntryToken(String userId) {
		Object value = redisTemplate.opsForValue().get(ENTRY_TOKEN_STORAGE_KEY_NAME + ":" + userId);
		return value == null ? null : value.toString();
	}

	public void updateEntryLastSeen(String userId, long epochMillis) {
		redisTemplate.opsForZSet().add(ENTRY_LAST_SEEN_KEY_NAME, userId, epochMillis);
	}

	public void refreshUserQueueEventTtl(String userId, long ttlSeconds) {
		redisTemplate.expire(USER_QUEUE_EVENT_KEY_NAME + ":" + userId, ttlSeconds,
			java.util.concurrent.TimeUnit.SECONDS);
	}

	public String getEventStatus(String eventId) {
		Object value = redisTemplate.opsForHash().get(EVENT_STATUSES_HASH_KEY, eventId);
		return value == null ? null : value.toString();
	}

	public PollingAdaptiveContext getPollingAdaptiveContext(String eventId, boolean includeWaitingQueueSize) {
		List<Object> values = redisTemplate.executePipelined(new SessionCallback<Object>() {
			@SuppressWarnings("unchecked")
			@Override
			public <K, V> Object execute(RedisOperations<K, V> operations) throws DataAccessException {
				RedisOperations<String, String> stringOperations =
					(RedisOperations<String, String>) operations;
				stringOperations.opsForHash().get(EVENT_STATUSES_HASH_KEY, eventId);
				stringOperations.opsForHash().get(ENTRY_QUEUE_SLOTS_KEY_NAME, eventId);
				if (includeWaitingQueueSize) {
					stringOperations.opsForZSet().size(WAITING_QUEUE_KEY_NAME + ":" + eventId);
				}
				return null;
			}
		});

		String eventStatus = toNullableString(values, 0);
		Long entryQueueSlots = toNullableLong(values, 1);
		Long waitingQueueSize = includeWaitingQueueSize ? toNullableLong(values, 2) : null;

		return new PollingAdaptiveContext(eventStatus, entryQueueSlots, waitingQueueSize);
	}

	public Long getEntryQueueSlots(String eventId) {
		Object value = redisTemplate.opsForHash().get(ENTRY_QUEUE_SLOTS_KEY_NAME, eventId);
		if (value == null) {
			return null;
		}
		try {
			return Long.parseLong(value.toString());
		} catch (NumberFormatException e) {
			return null;
		}
	}

	public Long getWaitingQueueSize(String eventId) {
		return redisTemplate.opsForZSet().size(WAITING_QUEUE_KEY_NAME + ":" + eventId);
	}

	private String toNullableString(List<Object> values, int index) {
		if (values == null || values.size() <= index) {
			return null;
		}

		Object value = values.get(index);
		return value == null ? null : value.toString();
	}

	private Long toNullableLong(List<Object> values, int index) {
		if (values == null || values.size() <= index) {
			return null;
		}

		Object value = values.get(index);
		if (value == null) {
			return null;
		}

		if (value instanceof Number number) {
			return number.longValue();
		}

		try {
			return Long.parseLong(value.toString());
		} catch (NumberFormatException e) {
			return null;
		}
	}
}
