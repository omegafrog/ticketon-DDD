package org.codenbug.broker.app;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.codenbug.broker.domain.Status;
import org.codenbug.broker.config.QueueProperties;
import org.codenbug.broker.service.SseConnection;
import org.codenbug.broker.service.SseEmitterService;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@Profile("mode-sse")
public class SSEEntryDispatchService implements EntryDispatcherService {
  private static final String ENTRY_QUEUE_SLOTS_KEY_NAME = "ENTRY_QUEUE_SLOTS";
  private static final String ENTRY_TOKEN_STORAGE_KEY_NAME = "ENTRY_TOKEN";

  public enum DispatchResult {
    ACK,
    SKIP_ACK
  }

  private final RedisTemplate<String, Object> redisTemplate;
  private final SseEmitterService sseEmitterService;
  private final EntryAuthService entryAuthService;
  private final QueueProperties queueProperties;
  private final QueueObservation queueObservation;

  public SSEEntryDispatchService(RedisTemplate<String, Object> redisTemplate,
      SseEmitterService sseEmitterService, EntryAuthService entryAuthService,
      QueueProperties queueProperties, QueueObservation queueObservation) {
    this.redisTemplate = redisTemplate;
    this.sseEmitterService = sseEmitterService;
    this.entryAuthService = entryAuthService;
    this.queueProperties = queueProperties;
    this.queueObservation = queueObservation;
  }

  @Override
  public DispatchResult handle(String userId, String eventId) {
    SseConnection sseConnection = sseEmitterService.getEmitterMap().get(userId);
    // dispatcher가 유저 승급했는데, 유저는 정작 연결 끊은 상태
    if (sseConnection == null) {
      handleDisconnectedUser(eventId, userId);
      return DispatchResult.ACK;
    }
    if (!sseConnection.getEventId().equals(eventId)) {
      return DispatchResult.SKIP_ACK;
    }

    processEntry(sseConnection, eventId, userId);
    return DispatchResult.ACK;
  }

  private void handleDisconnectedUser(String eventId, String userId) {
    log.info("count incremented");
    releaseSlot(eventId);
    redisTemplate.delete(buildEntryTokenKey(userId));
  }

  private void processEntry(SseConnection sseConnection, String eventId, String userId) {
    sseConnection.setStatus(Status.IN_PROGRESS);
    SseEmitter emitter = sseConnection.getEmitter();

    String token = entryAuthService
        .generateEntryAuthToken(Map.of("eventId", eventId, "userId", userId), "entryAuthToken");
    storeEntryToken(userId, eventId, token);

    try {
      emitter.send(SseEmitter.event().data(Map.of("eventId", eventId, "userId", userId, "status",
          sseConnection.getStatus(), "token", token)));
    } catch (IOException e) {
      sseEmitterService.closeConnection(userId, eventId);
    } catch (Exception e) {
      sseEmitterService.closeConnection(userId, eventId);
    }
  }

  private void storeEntryToken(String userId, String eventId, String token) {
    redisTemplate.opsForValue().set(buildEntryTokenKey(userId), token,
        queueProperties.getEntryTokenTtlMinutes(), TimeUnit.MINUTES);
    redisTemplate.opsForValue().set("ENTRY_EVENT:" + userId, eventId,
        queueProperties.getEntryTokenTtlMinutes(), TimeUnit.MINUTES);
    queueObservation.recordEntryTokenIssued(eventId);
  }

  private String buildEntryTokenKey(String userId) {
    return ENTRY_TOKEN_STORAGE_KEY_NAME + ":" + userId;
  }

  private void releaseSlot(String eventId) {
    Long released = redisTemplate.execute(new org.springframework.data.redis.core.script.DefaultRedisScript<>("""
        local current = redis.call("HGET", KEYS[1], ARGV[1])
        local max = tonumber(ARGV[2])
        if current == false then
          redis.call("HSET", KEYS[1], ARGV[1], max)
          return 1
        end
        local currentNumber = tonumber(current)
        if currentNumber == nil or currentNumber >= max then
          return 0
        end
        redis.call("HINCRBY", KEYS[1], ARGV[1], 1)
        return 1
        """, Long.class), java.util.List.of(ENTRY_QUEUE_SLOTS_KEY_NAME), eventId,
        String.valueOf(queueProperties.getMaxActiveShoppers()));
    queueObservation.recordSlotReleased(eventId, released != null && released > 0);
  }
}
