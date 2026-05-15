package org.codenbug.infra.redis;

import java.util.HashSet;
import java.util.Set;

import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class RedisKeyScanner {
	private final RedisTemplate<String, Object> redisTemplate;

	public Set<String> scanKeys(String pattern) {
		Set<String> keys = new HashSet<>();
		ScanOptions options = ScanOptions.scanOptions().match(pattern).count(100).build();

		try (Cursor<byte[]> cursor = redisTemplate.getConnectionFactory()
			.getConnection()
			.scan(options)) {
			while (cursor.hasNext()) {
				keys.add(new String(cursor.next()));
			}
		} catch (Exception e) {
			log.error("Error scanning keys with pattern: {}", pattern, e);
		}
		return keys;
	}

	public Set<String> keys(String pattern) {
		try {
			return redisTemplate.keys(pattern);
		} catch (Exception e) {
			log.error("Error getting keys with pattern: {}", pattern, e);
			return new HashSet<>();
		}
	}
}