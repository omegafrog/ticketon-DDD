package org.codenbug.purchase.global;

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
		String redisKey = ENTRY_TOKEN_STORAGE_KEY_NAME;
		String storedToken = (String)redisTemplate.opsForHash().get(redisKey, userId.toString());

		if (storedToken == null) {
			throw new AccessDeniedException("유효하지 않은 입장 토큰입니다.");
		}

		storedToken = storedToken.replace("\"", "");  // 쌍따옴표 제거

		if (!storedToken.equals(token)) {
			throw new AccessDeniedException("유효하지 않은 입장 토큰입니다.");
		}
	}

}

