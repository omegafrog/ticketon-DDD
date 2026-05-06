package org.codenbug.purchase.domain.port;

import java.util.List;

public interface MessagePublisher {
	void publishSeatPurchasedEvent(String eventId, Long layoutId, List<String> seatIds, String userId);

	void publishSeatPurchaseCanceledEvent(List<String> seatIds, String value);
}
