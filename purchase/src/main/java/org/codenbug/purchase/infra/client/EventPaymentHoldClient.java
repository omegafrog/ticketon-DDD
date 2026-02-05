package org.codenbug.purchase.infra.client;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

@Component
public class EventPaymentHoldClient {
	private final RestTemplate restTemplate;
	private final String eventServiceBaseUrl;

	public EventPaymentHoldClient(
		@Qualifier("purchaseRestTemplate") RestTemplate restTemplate,
		@Value("${services.event.base-url}") String eventServiceBaseUrl
	) {
		this.restTemplate = restTemplate;
		this.eventServiceBaseUrl = eventServiceBaseUrl;
	}

	public EventPaymentHoldCreateResponse createHold(String eventId, Long expectedSalesVersion, int ttlSeconds,
		String purchaseId) {
		String url = "%s/internal/events/%s/payment-holds".formatted(eventServiceBaseUrl, eventId);
		EventPaymentHoldCreateRequest body = new EventPaymentHoldCreateRequest(expectedSalesVersion, ttlSeconds, purchaseId);

		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		HttpEntity<EventPaymentHoldCreateRequest> entity = new HttpEntity<>(body, headers);
		ResponseEntity<EventPaymentHoldCreateResponse> response = restTemplate.exchange(
			url,
			HttpMethod.POST,
			entity,
			EventPaymentHoldCreateResponse.class
		);
		return response.getBody();
	}

	public void consumeHold(String eventId, String holdToken) {
		String url = "%s/internal/events/%s/payment-holds/%s/consume".formatted(eventServiceBaseUrl, eventId, holdToken);
		restTemplate.postForEntity(url, null, Void.class);
	}

	public void releaseHold(String eventId, String holdToken) {
		String url = "%s/internal/events/%s/payment-holds/%s/release".formatted(eventServiceBaseUrl, eventId, holdToken);
		restTemplate.postForEntity(url, null, Void.class);
	}

	public static boolean isHoldRejected(HttpClientErrorException ex) {
		return ex.getStatusCode().value() == 409;
	}
}
