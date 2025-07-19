package org.codenbug.purchase.infra;

import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.stereotype.Component;

@Component
public class RedisKeyScanner {

	private final RedisTemplate<String, String> redisTemplate;

	public RedisKeyScanner(@Qualifier("simpleRedisTemplate") RedisTemplate<String, String> redisTemplate) {
		this.redisTemplate = redisTemplate;
	}

	/**
	 * 주어진 패턴에 매칭되는 Redis 키를 안전하게 조회합니다.
	 *
	 * @param pattern 스캔할 키 패턴 (예: "seat:lock:26:*")
	 * @return 일치하는 키 목록
	 */
	public Set<String> scanKeys(String pattern) {
		return redisTemplate.execute((RedisCallback<Set<String>>)connection -> {
			Set<String> result = new HashSet<>();

			ScanOptions options = ScanOptions.scanOptions()
				.match(pattern)
				.count(1000)
				.build();

			try (var cursor = connection.scan(options)) {
				while (cursor.hasNext()) {
					result.add(new String(cursor.next(), StandardCharsets.UTF_8));
				}
			}

			return result;
		});
	}
}
