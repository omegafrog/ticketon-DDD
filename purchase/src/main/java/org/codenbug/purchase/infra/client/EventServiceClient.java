package org.codenbug.purchase.infra.client;

import org.codenbug.purchase.domain.EventInfoProvider;
import org.codenbug.purchase.domain.EventSummary;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component("purchaseEventServiceClient")
public class EventServiceClient implements EventInfoProvider {
	private final RestTemplate restTemplate;
	private final String eventServiceBaseUrl;

	public EventServiceClient(@org.springframework.beans.factory.annotation.Qualifier("purchaseRestTemplate") RestTemplate restTemplate,
		@Value("${services.event.base-url}") String eventServiceBaseUrl) {
		this.restTemplate = restTemplate;
		this.eventServiceBaseUrl = eventServiceBaseUrl;
	}

	@Override
	public EventSummary getEventSummary(String eventId) {
		String url = "%s/internal/events/%s/summary".formatted(eventServiceBaseUrl, eventId);
		EventSummaryResponse response = restTemplate.getForObject(url, EventSummaryResponse.class);
		if (response == null) {
			throw new IllegalArgumentException("이벤트 정보를 찾을 수 없습니다.");
		}
		return new EventSummary(
			response.getEventId(),
			response.getSeatLayoutId(),
			response.isSeatSelectable(),
			response.getStatus(),
			response.getVersion(),
			response.getTitle()
		);
	}
}
