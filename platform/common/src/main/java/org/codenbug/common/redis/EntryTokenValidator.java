package org.codenbug.common.redis;

import org.codenbug.common.exception.AccessDeniedException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class EntryTokenValidator {
	private final RedisTemplate<String, Object> redisTemplate;
	public static final String ENTRY_TOKEN_STORAGE_KEY_NAME = "ENTRY_TOKEN";

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

}

