package org.codenbug.purchase.app;

import org.codenbug.purchase.event.ManagerRefundCompletedEvent;
import org.codenbug.purchase.event.RefundCompletedEvent;

public interface RefundNotificationPort {
	void publishRefundCompletedEvent(RefundCompletedEvent event);

	void publishManagerRefundCompletedEvent(ManagerRefundCompletedEvent event);
}
