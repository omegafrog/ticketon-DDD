package org.codenbug.purchase.infra.client;

import org.codenbug.common.RsData;
import org.codenbug.purchase.domain.EventInfoProvider;
import org.codenbug.purchase.domain.EventSummary;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component("purchaseEventServiceClient")
public class EventServiceClient implements EventInfoProvider {
  private final RestTemplate restTemplate;
  private final String eventServiceBaseUrl;

  public EventServiceClient(
      @Qualifier("purchaseRestTemplate") RestTemplate restTemplate,
      @Value("${services.event.base-url}") String eventServiceBaseUrl) {
    this.restTemplate = restTemplate;
    this.eventServiceBaseUrl = eventServiceBaseUrl;
  }

  @Override
  public EventSummary getEventSummary(String eventId) {
    String url = "%s/internal/events/%s/summary".formatted(eventServiceBaseUrl, eventId);
    ResponseEntity<RsData<EventSummaryResponse>> responseEntity = restTemplate.exchange(
        url,
        HttpMethod.GET,
        HttpEntity.EMPTY,
        new ParameterizedTypeReference<RsData<EventSummaryResponse>>() {
        });
    log.info("response:{}", responseEntity);

    EventSummaryResponse response = responseEntity.getBody().getData();
    if (response == null) {
      throw new IllegalArgumentException("이벤트 정보를 찾을 수 없습니다.");
    }
    return new EventSummary(
        response.getEventId(),
        response.getSeatLayoutId(),
        response.isSeatSelectable(),
        response.getStatus(),
        response.getVersion(),
        response.getSalesVersion(),
        response.getTitle(),
        response.getManagerId());
  }

  @Override
  public boolean isEventStateValid(String eventId, Long version, String status) {
    String url = "%s/internal/events/%s/version-check?version=%d&status=%s"
        .formatted(eventServiceBaseUrl, eventId, version, status);
    Boolean response = restTemplate.getForObject(url, Boolean.class);
    return response != null && response;
  }
}
