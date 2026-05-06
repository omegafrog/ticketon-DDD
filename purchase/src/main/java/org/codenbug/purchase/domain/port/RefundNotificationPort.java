package org.codenbug.purchase.domain.port;

import org.codenbug.purchase.domain.event.ManagerRefundCompletedEvent;
import org.codenbug.purchase.domain.event.RefundCompletedEvent;

public interface RefundNotificationPort {
	void publishRefundCompletedEvent(RefundCompletedEvent event);

	void publishManagerRefundCompletedEvent(ManagerRefundCompletedEvent event);
}
