package org.codenbug.purchase.domain;

public interface EventInfoProvider {
	EventSummary getEventSummary(String eventId);
	boolean isEventStateValid(String eventId, Long version, String status);
}
