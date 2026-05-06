package org.codenbug.purchase.domain.port;

import org.codenbug.purchase.domain.EventSummary;
public interface EventInfoProvider {
	EventSummary getEventSummary(String eventId);
	boolean isEventStateValid(String eventId, Long version, String status);
}
