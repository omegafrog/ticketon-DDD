package org.codenbug.broker.app;

public interface QueueObservation {
	void recordPollingRequest(String eventId, String state, long pollAfterMs);

	void recordQueueState(String eventId, Long waitingUsers, Long entrySlots);

	static QueueObservation noop() {
		return NoopQueueObservation.INSTANCE;
	}
}
