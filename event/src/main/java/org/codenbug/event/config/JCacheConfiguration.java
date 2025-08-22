package org.codenbug.event.config;

import javax.cache.Caching;
import javax.cache.configuration.MutableConfiguration;
import javax.cache.expiry.CreatedExpiryPolicy;
import javax.cache.expiry.Duration;
import javax.cache.spi.CachingProvider;

import org.codenbug.event.domain.Event;
import org.codenbug.event.query.EventListProjection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

//@Configuration
public class JCacheConfiguration {

	@Bean
	public javax.cache.CacheManager jCacheManager() {
		System.setProperty("javax.cache.spi.CachingProvider",
			"org.redisson.jcache.JCachingProvider");

		CachingProvider provider = Caching.getCachingProvider();
		javax.cache.CacheManager cacheManager = provider.getCacheManager();

		// Event 캐시 설정
		MutableConfiguration<String, EventListProjection> eventConfig =
			new MutableConfiguration<String, EventListProjection>()
				.setTypes(String.class, EventListProjection.class)
				.setExpiryPolicyFactory(CreatedExpiryPolicy.factoryOf(Duration.THIRTY_MINUTES))
				.setStatisticsEnabled(true);

		cacheManager.createCache("events", eventConfig);

		return cacheManager;
	}
}