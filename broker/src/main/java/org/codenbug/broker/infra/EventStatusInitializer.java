package org.codenbug.broker.infra;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class EventStatusInitializer {

    private static final String EVENT_STATUSES_HASH_KEY = "event_statuses";

    private final StringRedisTemplate stringRedisTemplate;
    private final RestTemplate restTemplate;
    private final String eventServiceBaseUrl;

    public EventStatusInitializer(
        StringRedisTemplate stringRedisTemplate,
        RestTemplate restTemplate,
        @Value("${services.event.base-url:${EVENT_SERVICE_BASE_URL:http://event}}") String eventServiceBaseUrl
    ) {
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
            EventSummaryResponse response = restTemplate.getForObject(url, EventSummaryResponse.class);
            if (response == null || response.getStatus() == null || response.getStatus().isBlank()) {
                throw new IllegalStateException("이벤트 상태를 조회할 수 없습니다.");
            }
            stringRedisTemplate.opsForHash().putIfAbsent(EVENT_STATUSES_HASH_KEY, eventId, response.getStatus());
        }catch (Exception e){
            throw new IllegalStateException(e);
        }

    }

    public static class EventSummaryResponse {
        private String status;

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }
    }
}
