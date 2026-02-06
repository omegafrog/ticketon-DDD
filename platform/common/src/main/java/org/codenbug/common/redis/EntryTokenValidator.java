package org.codenbug.common.redis;

import org.codenbug.common.exception.AccessDeniedException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class EntryTokenValidator {
	private final RedisTemplate<String, Object> redisTemplate;
	public static final String ENTRY_TOKEN_STORAGE_KEY_NAME = "ENTRY_TOKEN";
	public static final String ENTRY_EVENT_STORAGE_KEY_NAME = "ENTRY_EVENT";

	public EntryTokenValidator(RedisTemplate<String, Object> redisTemplate) {
		this.redisTemplate = redisTemplate;
	}

	public void validate(String userId, String token) {
		String redisKey = ENTRY_TOKEN_STORAGE_KEY_NAME + ":" + userId;
		Object storedTokenObj = redisTemplate.opsForValue().get(redisKey);

		if (storedTokenObj == null) {
			throw new AccessDeniedException("유효하지 않은 입장 토큰입니다.");
		}

		String storedToken = storedTokenObj.toString().replace("\"", "");

		if (!storedToken.equals(token)) {
			throw new AccessDeniedException("유효하지 않은 입장 토큰입니다.");
		}
	}

	public void validate(String userId, String token, String eventId) {
		validate(userId, token);

		if (eventId == null || eventId.isBlank()) {
			throw new AccessDeniedException("유효하지 않은 입장 토큰입니다.");
		}

		String eventKey = ENTRY_EVENT_STORAGE_KEY_NAME + ":" + userId;
		Object storedEventIdObj = redisTemplate.opsForValue().get(eventKey);
		if (storedEventIdObj == null) {
			throw new AccessDeniedException("유효하지 않은 입장 토큰입니다.");
		}
		String storedEventId = storedEventIdObj.toString().replace("\"", "");
		if (!storedEventId.equals(eventId)) {
			throw new AccessDeniedException("유효하지 않은 입장 토큰입니다.");
		}
	}

}

