package org.codenbug.broker.app;

public interface QueueObservation {
	void recordPollingRequest(String eventId, String state, long pollAfterMs);

	void recordQueueState(String eventId, Long waitingUsers, Long entrySlots);

	void recordEntryTokenIssued(String eventId);

	void recordEntryTokenExpired(String eventId);

	void recordSlotReleased(String eventId, boolean released);

	static QueueObservation noop() {
		return NoopQueueObservation.INSTANCE;
	}
}
