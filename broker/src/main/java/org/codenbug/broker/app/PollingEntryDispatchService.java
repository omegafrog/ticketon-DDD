package org.codenbug.broker.app;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Profile("polling")
@Component
@Slf4j
public class PollingEntryDispatchService implements EntryDispatcherService {
  private static final String ENTRY_TOKEN_STORAGE_KEY_NAME = "ENTRY_TOKEN";
  private static final long ENTRY_TOKEN_TTL_MINUTES = 60;

  private final EntryAuthService entryAuthService;
  private final StringRedisTemplate redisTemplate;

  public PollingEntryDispatchService(EntryAuthService entryAuthService, StringRedisTemplate redisTemplate) {
    this.entryAuthService = entryAuthService;
    this.redisTemplate = redisTemplate;
  }

  public SSEEntryDispatchService.DispatchResult handle(String userId, String eventId) {
    log.info("polling token create start.");
    processEntry(eventId, userId);
    return SSEEntryDispatchService.DispatchResult.ACK;
  }

  private void processEntry(String eventId, String userId) {

    String token = entryAuthService
        .generateEntryAuthToken(Map.of("eventId", eventId, "userId", userId), "entryAuthToken");
    storeEntryToken(userId, token);
  }

  private void storeEntryToken(String userId, String token) {
    redisTemplate.opsForValue().set(buildEntryTokenKey(userId), token, ENTRY_TOKEN_TTL_MINUTES, TimeUnit.MINUTES);
  }

  private String buildEntryTokenKey(String userId) {
    return ENTRY_TOKEN_STORAGE_KEY_NAME + ":" + userId;
  }
}
