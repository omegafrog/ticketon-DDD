package org.codenbug.broker.app;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.codenbug.broker.config.QueueProperties;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Profile("polling")
@Component
@Slf4j
public class PollingEntryDispatchService implements EntryDispatcherService {
  private static final String ENTRY_TOKEN_STORAGE_KEY_NAME = "ENTRY_TOKEN";

  private final EntryAuthService entryAuthService;
  private final StringRedisTemplate redisTemplate;
  private final QueueProperties queueProperties;
  private final QueueObservation queueObservation;

  public PollingEntryDispatchService(EntryAuthService entryAuthService, StringRedisTemplate redisTemplate,
      QueueProperties queueProperties, QueueObservation queueObservation) {
    this.entryAuthService = entryAuthService;
    this.redisTemplate = redisTemplate;
    this.queueProperties = queueProperties;
    this.queueObservation = queueObservation;
  }

  public SSEEntryDispatchService.DispatchResult handle(String userId, String eventId) {
    log.info("polling token create start.");
    processEntry(eventId, userId);
    return SSEEntryDispatchService.DispatchResult.ACK;
  }

  private void processEntry(String eventId, String userId) {

    String token = entryAuthService
        .generateEntryAuthToken(Map.of("eventId", eventId, "userId", userId), "entryAuthToken");
    storeEntryToken(userId, eventId, token);
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
}
