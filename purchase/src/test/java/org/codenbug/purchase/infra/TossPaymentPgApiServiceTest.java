package org.codenbug.purchase.infra;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import org.codenbug.purchase.config.TossPaymentProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

@ExtendWith(MockitoExtension.class)
class TossPaymentPgApiServiceTest {

	@Mock
	private RestTemplate restTemplate;

	@Test
	void Toss_결제승인에는_confirm_구매아이디를_Idempotency_Key로_전달한다() {
		TossPaymentProperties properties = new TossPaymentProperties();
		properties.setSecretKey("test-secret");
		properties.setApiUrl("https://api.toss.test/v1/payments");
		TossPaymentPgApiService service = new TossPaymentPgApiService(restTemplate, properties);
		when(restTemplate.exchange(eq("https://api.toss.test/v1/payments/confirm"), eq(HttpMethod.POST),
			org.mockito.ArgumentMatchers.<HttpEntity<Map<String, Object>>>any(), eq(byte[].class)))
			.thenReturn(ResponseEntity.ok("""
				{"paymentKey":"payKey","orderId":"order-1","orderName":"테스트 주문","totalAmount":1000,"status":"DONE","method":"CARD","approvedAt":"2026-03-26T08:00:00+09:00","receipt":{"url":"https://receipt.example.com"}}
				""".trim().getBytes(StandardCharsets.UTF_8)));

		service.confirmPayment("payKey", "order-1", 1000, "confirm:purchase-1");

		@SuppressWarnings("unchecked")
		ArgumentCaptor<HttpEntity<Map<String, Object>>> captor = ArgumentCaptor.forClass(HttpEntity.class);
		verify(restTemplate).exchange(eq("https://api.toss.test/v1/payments/confirm"), eq(HttpMethod.POST),
			captor.capture(), eq(byte[].class));
		assertThat(captor.getValue().getHeaders().getFirst("Idempotency-Key")).isEqualTo("confirm:purchase-1");
	}
}
