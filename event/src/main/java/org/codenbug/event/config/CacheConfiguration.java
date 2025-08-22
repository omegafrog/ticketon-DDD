package org.codenbug.event.config;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.redisson.spring.cache.CacheConfig;
import org.redisson.spring.cache.RedissonSpringCacheManager;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableCaching
public class CacheConfiguration {

	@Bean
	public RedissonClient redissonClient() {
		Config config = new Config();
		config.useSingleServer()
			.setAddress("redis://localhost:6380")
			.setConnectionPoolSize(10)
			.setConnectionMinimumIdleSize(5)
			.setTimeout(3000)
			.setRetryAttempts(3);

		return Redisson.create(config);
	}

	@Bean
	public CacheManager cacheManager(RedissonClient redissonClient) {
		Map<String, CacheConfig> configMap = new HashMap<>();

		// Event 단건 캐시 설정
		configMap.put("eventCache", new CacheConfig(
			30*60,  // TTL
			10*60   // Max Idle Time
		));

		// Event List 캐시 설정
		configMap.put("eventListCache", new CacheConfig(
			5*60,
			2*60
		));

		return new RedissonSpringCacheManager(redissonClient, configMap);
	}
}
