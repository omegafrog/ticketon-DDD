package org.codenbug.purchase.domain;

public interface EventInfoProvider {
	EventSummary getEventSummary(String eventId);
}
