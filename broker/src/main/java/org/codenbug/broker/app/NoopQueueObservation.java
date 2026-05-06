package org.codenbug.broker.app;

enum NoopQueueObservation implements QueueObservation {
	INSTANCE;

	@Override
	public void recordPollingRequest(String eventId, String state, long pollAfterMs) {
	}

	@Override
	public void recordQueueState(String eventId, Long waitingUsers, Long entrySlots) {
	}
}
