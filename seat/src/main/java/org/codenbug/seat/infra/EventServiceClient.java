package org.codenbug.seat.infra;

import org.codenbug.common.RsData;
import org.codenbug.seat.app.EventSeatLayoutPort;
import org.codenbug.seat.app.EventSeatLayoutSummary;
import org.codenbug.seat.infra.dto.EventSummaryResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component("seatEventServiceClient")
public class EventServiceClient implements EventSeatLayoutPort {
	private final RestTemplate restTemplate;
	private final String eventServiceBaseUrl;

	public EventServiceClient(@org.springframework.beans.factory.annotation.Qualifier("seatRestTemplate") RestTemplate restTemplate,
		@Value("${services.event.base-url}") String eventServiceBaseUrl) {
		this.restTemplate = restTemplate;
		this.eventServiceBaseUrl = eventServiceBaseUrl;
	}

	public EventSeatLayoutSummary getEventSummary(String eventId) {
		String url = "%s/internal/events/%s/summary".formatted(eventServiceBaseUrl, eventId);
		ResponseEntity<RsData<EventSummaryResponse>> responseEntity = restTemplate.exchange(
			url,
			HttpMethod.GET,
			null,
			new ParameterizedTypeReference<>() {
			}
		);
		RsData<EventSummaryResponse> body = responseEntity.getBody();
		EventSummaryResponse response = body == null ? null : body.getData();
		if (response == null || response.getSeatLayoutId() == null) {
			throw new IllegalArgumentException("이벤트 정보를 찾을 수 없습니다.");
		}
		return new EventSeatLayoutSummary(response.getSeatLayoutId(), response.isSeatSelectable());
	}
}
