package org.codenbug.broker.infra;

import static org.codenbug.broker.infra.RedisConfig.*;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import lombok.extern.slf4j.Slf4j;

import org.codenbug.broker.app.EventStatusInitializationPort;
import org.codenbug.common.RsData;

@Component
@Slf4j
public class EventStatusInitializer implements EventStatusInitializationPort {

  private final StringRedisTemplate stringRedisTemplate;
  private final RestTemplate restTemplate;
  private final String eventServiceBaseUrl;

  public EventStatusInitializer(
      StringRedisTemplate stringRedisTemplate,
      RestTemplate restTemplate,
      @Value("${services.event.base-url:${EVENT_SERVICE_BASE_URL:http://app}}") String eventServiceBaseUrl) {
    this.stringRedisTemplate = stringRedisTemplate;
    this.restTemplate = restTemplate;
    this.eventServiceBaseUrl = eventServiceBaseUrl;
  }

  public void ensureInitialized(String eventId) {
    Boolean exists = stringRedisTemplate.opsForHash().hasKey(EVENT_STATUSES_HASH_KEY, eventId);
    if (Boolean.TRUE.equals(exists)) {
      return;
    }

    String url = "%s/internal/events/%s/summary".formatted(eventServiceBaseUrl, eventId);

    try {
      ResponseEntity<RsData<EventSummaryResponse>> responseEntity = restTemplate.exchange(
          url,
          HttpMethod.GET,
          HttpEntity.EMPTY,
          new ParameterizedTypeReference<RsData<EventSummaryResponse>>() {
          });

      EventSummaryResponse response = responseEntity.getBody().getData();
      if (response == null || response.getStatus() == null || response.getStatus().isBlank()) {
        throw new IllegalStateException("이벤트 상태를 조회할 수 없습니다.");
      }
      stringRedisTemplate.opsForHash().putIfAbsent(EVENT_STATUSES_HASH_KEY, eventId, response.getStatus());
    } catch (Exception e) {
      log.info(e.getMessage());
      throw new IllegalStateException(e);
    }

  }

}
