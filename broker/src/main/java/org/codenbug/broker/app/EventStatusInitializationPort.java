package org.codenbug.broker.app;

public interface EventStatusInitializationPort {
	void ensureInitialized(String eventId);
}
