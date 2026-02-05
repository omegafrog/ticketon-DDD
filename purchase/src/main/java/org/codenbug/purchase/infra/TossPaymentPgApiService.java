package org.codenbug.purchase.infra;

import java.util.Base64;
import java.util.Map;

import org.codenbug.purchase.app.PGApiService;
import org.codenbug.purchase.app.PaymentProvider;
import org.codenbug.purchase.config.TossPaymentProperties;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class TossPaymentPgApiService implements PGApiService {
	private static final PaymentProvider TOSS = PaymentProvider.TOSS;
	private final RestTemplate restTemplate;
	private final String secretKey;
	private final String tossApiUrl;

	public TossPaymentPgApiService(@Qualifier("purchaseRestTemplate") RestTemplate restTemplate,
		TossPaymentProperties tossPaymentProperties){
		this.restTemplate = restTemplate;
		this.secretKey = tossPaymentProperties.getSecretKey();
		this.tossApiUrl = tossPaymentProperties.getApiUrl();
	}

	private HttpHeaders createAuthHeaders() {
		return createAuthHeaders(null);
	}

	private HttpHeaders createAuthHeaders(String idempotencyKey) {
		HttpHeaders headers = new HttpHeaders();
		String encoded = Base64.getEncoder().encodeToString((secretKey + ":").getBytes());
		headers.set("Authorization", "Basic " + encoded);
		headers.setContentType(MediaType.APPLICATION_JSON);
		headers.setAcceptCharset(java.util.Collections.singletonList(java.nio.charset.StandardCharsets.UTF_8));
		if (idempotencyKey != null && !idempotencyKey.isBlank()) {
			headers.set("Idempotency-Key", idempotencyKey);
		}

		return headers;
	}

	/**
	 * Toss 서버에 결제 승인을 요청하고 결과 정보를 반환
	 */
	@Override
	public ConfirmedPaymentInfo confirmPayment(String paymentKey, String orderId, Integer amount) {
		return confirmPayment(paymentKey, orderId, amount, null);
	}

	@Override
	public ConfirmedPaymentInfo confirmPayment(String paymentKey, String orderId, Integer amount, String idempotencyKey) {
		String url = tossApiUrl + "/confirm";
		Map<String, Object> body = Map.of(
			"paymentKey", paymentKey,
			"orderId", orderId,
			"amount", amount
		);
		return postToToss(url, body, ConfirmedPaymentInfo.class, idempotencyKey);
	}

	/**
	 * Toss 서버에 전액 결제 취소를 요청하고 결과 정보를 반환
	 */
	@Override
	public CanceledPaymentInfo cancelPayment(String paymentKey, String cancelReason) {
		return cancelPayment(paymentKey, cancelReason, null);
	}

	@Override
	public CanceledPaymentInfo cancelPayment(String paymentKey, String cancelReason, String idempotencyKey) {
		String url = tossApiUrl + "/" + paymentKey + "/cancel";
		Map<String, Object> body = Map.of("cancelReason", cancelReason);
		return postToToss(url, body, CanceledPaymentInfo.class, idempotencyKey);
	}

	/**
	 * 공통 Toss 서버 요청 로직
	 */
	private <T> T postToToss(String url, Map<String, Object> body, Class<T> clazz, String idempotencyKey) {
		try {
			HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, createAuthHeaders(idempotencyKey));
			ResponseEntity<byte[]> response = restTemplate.exchange(url, HttpMethod.POST, request, byte[].class);
			
			String responseBody = new String(response.getBody(), java.nio.charset.StandardCharsets.UTF_8);
			ObjectMapper objectMapper = new ObjectMapper();

			return objectMapper.readValue(responseBody, clazz);
		} catch (Exception e) {
			throw new RuntimeException("Toss 응답 파싱 실패: " + e.getMessage(), e);
		}
	}
	@Override
	public boolean supports(PaymentProvider provider) {
		return TOSS.equals(provider);
	}
}
