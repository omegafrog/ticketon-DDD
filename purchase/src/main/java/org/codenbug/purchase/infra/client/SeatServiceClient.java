package org.codenbug.purchase.infra.client;

import java.util.List;

import org.codenbug.purchase.domain.SeatInfo;
import org.codenbug.purchase.domain.SeatLayoutInfo;
import org.codenbug.purchase.domain.SeatLayoutProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class SeatServiceClient implements SeatLayoutProvider {
	private final RestTemplate restTemplate;
	private final String seatServiceBaseUrl;

	public SeatServiceClient(@org.springframework.beans.factory.annotation.Qualifier("purchaseRestTemplate") RestTemplate restTemplate,
		@Value("${services.seat.base-url}") String seatServiceBaseUrl) {
		this.restTemplate = restTemplate;
		this.seatServiceBaseUrl = seatServiceBaseUrl;
	}

	@Override
	public SeatLayoutInfo getSeatLayout(Long seatLayoutId) {
		String url = "%s/internal/seat-layouts/%s".formatted(seatServiceBaseUrl, seatLayoutId);
		SeatLayoutResponse response = restTemplate.getForObject(url, SeatLayoutResponse.class);
		if (response == null) {
			throw new IllegalArgumentException("좌석 레이아웃 정보를 찾을 수 없습니다.");
		}

		List<SeatInfo> seats = response.getSeats().stream()
			.map(seat -> new SeatInfo(
				seat.getId(),
				seat.getSignature(),
				seat.getGrade(),
				seat.getPrice(),
				seat.isAvailable()
			))
			.toList();

		return new SeatLayoutInfo(response.getId(), response.getLocationName(), response.getHallName(), seats);
	}
}
