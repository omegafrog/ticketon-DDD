package org.codenbug.common.redis;

import java.util.HashSet;
import java.util.Set;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Utility class for scanning Redis keys
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RedisKeyScanner {

    @Qualifier("simpleRedisTemplate")
    private final RedisTemplate<String, String> redisTemplate;

    /**
     * Scan Redis keys matching the given pattern
     * Uses the SCAN command with a cursor to safely scan large key spaces
     *
     * @param pattern The pattern to match keys against
     * @return A set of keys matching the pattern
     */
    public Set<String> scanKeys(String pattern) {
        return redisTemplate.execute((RedisCallback<Set<String>>) connection -> {
            Set<String> keys = new HashSet<>();
            Cursor<byte[]> cursor = connection.keyCommands().scan(
                    ScanOptions.scanOptions().match(pattern).count(100).build());
            
            while (cursor.hasNext()) {
                keys.add(new String(cursor.next()));
            }
            
            try {
                cursor.close();
            } catch (Exception e) {
                log.error("Error closing Redis cursor", e);
            }
            
            return keys;
        });
    }
}