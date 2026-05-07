package org.codenbug.messagedispatcher.thread;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.concurrent.atomic.AtomicLong;

import org.codenbug.messagedispatcher.config.QueueProperties;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.test.util.ReflectionTestUtils;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

class EntryPromoterTest {

  @Test
  void dispatcherPromotesNoMoreThanBatchAndRateBudgetPerTick() {
    RedisTemplate<String, Object> redisTemplate = mock(RedisTemplate.class);
    StringRedisTemplate stringRedisTemplate = mock(StringRedisTemplate.class);
    HashOperations<String, Object, Object> hashOperations = mock(HashOperations.class);
    QueueProperties queueProperties = new QueueProperties();
    queueProperties.setPromotionBatchSize(50);
    queueProperties.setNewUsersPerMinute(3000);
    queueProperties.setPromotionIntervalMs(1000);
    AtomicLong promotionCounter = new AtomicLong();

    when(stringRedisTemplate.opsForHash()).thenReturn(hashOperations);
    when(hashOperations.get("event_statuses", "event-1")).thenReturn("OPEN");
    doReturn(50L).when(redisTemplate).execute(any(DefaultRedisScript.class), anyList(),
        eq("event-1"), eq("50"), eq("50"));

    EntryPromoter promoter = new EntryPromoter(redisTemplate, stringRedisTemplate, new ObjectMapper(),
        promotionCounter, queueProperties, new SimpleMeterRegistry());

    ReflectionTestUtils.invokeMethod(promoter, "executePromotionScript", "event-1");

    verify(redisTemplate).execute(any(DefaultRedisScript.class), anyList(),
        eq("event-1"), eq("50"), eq("50"));
    org.assertj.core.api.Assertions.assertThat(promotionCounter.get()).isEqualTo(50);

    promoter.shutdown();
  }

  @Test
  void promotionRateBudgetUsesConfiguredMinuteRateAndInterval() {
    RedisTemplate<String, Object> redisTemplate = mock(RedisTemplate.class);
    StringRedisTemplate stringRedisTemplate = mock(StringRedisTemplate.class);
    QueueProperties queueProperties = new QueueProperties();
    queueProperties.setNewUsersPerMinute(600);
    queueProperties.setPromotionIntervalMs(1000);

    EntryPromoter promoter = new EntryPromoter(redisTemplate, stringRedisTemplate, new ObjectMapper(),
        new AtomicLong(), queueProperties, new SimpleMeterRegistry());

    Integer budget = ReflectionTestUtils.invokeMethod(promoter, "promotionRateBudgetForTick");

    org.assertj.core.api.Assertions.assertThat(budget).isEqualTo(10);

    promoter.shutdown();
  }
}
