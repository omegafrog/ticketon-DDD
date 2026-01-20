package org.codenbug.seat.infra;

import org.codenbug.seat.infra.dto.EventSummaryResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component("seatEventServiceClient")
public class EventServiceClient {
	private final RestTemplate restTemplate;
	private final String eventServiceBaseUrl;

	public EventServiceClient(@org.springframework.beans.factory.annotation.Qualifier("seatRestTemplate") RestTemplate restTemplate,
		@Value("${services.event.base-url}") String eventServiceBaseUrl) {
		this.restTemplate = restTemplate;
		this.eventServiceBaseUrl = eventServiceBaseUrl;
	}

	public EventSummaryResponse getEventSummary(String eventId) {
		String url = "%s/internal/events/%s/summary".formatted(eventServiceBaseUrl, eventId);
		EventSummaryResponse response = restTemplate.getForObject(url, EventSummaryResponse.class);
		if (response == null) {
			throw new IllegalArgumentException("이벤트 정보를 찾을 수 없습니다.");
		}
		return response;
	}
}
