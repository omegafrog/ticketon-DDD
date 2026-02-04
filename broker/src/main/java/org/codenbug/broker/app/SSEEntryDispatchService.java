package org.codenbug.broker.app;

import static org.codenbug.broker.infra.RedisConfig.*;
import static org.codenbug.broker.service.SseEmitterService.*;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.codenbug.broker.domain.SseConnection;
import org.codenbug.broker.domain.Status;
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

  public enum DispatchResult {
    ACK,
    SKIP_ACK
  }

  private final RedisTemplate<String, Object> redisTemplate;
  private final SseEmitterService sseEmitterService;
  private final EntryAuthService entryAuthService;

  public SSEEntryDispatchService(RedisTemplate<String, Object> redisTemplate,
      SseEmitterService sseEmitterService, EntryAuthService entryAuthService) {
    this.redisTemplate = redisTemplate;
    this.sseEmitterService = sseEmitterService;
    this.entryAuthService = entryAuthService;
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
    redisTemplate.opsForHash().increment(ENTRY_QUEUE_SLOTS_KEY_NAME, eventId, 1);
    redisTemplate.delete(buildEntryTokenKey(userId));
  }

  private void processEntry(SseConnection sseConnection, String eventId, String userId) {
    sseConnection.setStatus(Status.IN_PROGRESS);
    SseEmitter emitter = sseConnection.getEmitter();

    String token = entryAuthService
        .generateEntryAuthToken(Map.of("eventId", eventId, "userId", userId), "entryAuthToken");
    storeEntryToken(userId, token);

    try {
      emitter.send(SseEmitter.event().data(Map.of("eventId", eventId, "userId", userId, "status",
          sseConnection.getStatus(), "token", token)));
    } catch (IOException e) {
      closeConn(userId, eventId, redisTemplate);
    } catch (Exception e) {
      closeConn(userId, eventId, redisTemplate);
    }
  }

  private void storeEntryToken(String userId, String token) {
    redisTemplate.opsForValue().set(buildEntryTokenKey(userId), token, 5, TimeUnit.MINUTES);
  }

  private String buildEntryTokenKey(String userId) {
    return ENTRY_TOKEN_STORAGE_KEY_NAME + ":" + userId;
  }
}
