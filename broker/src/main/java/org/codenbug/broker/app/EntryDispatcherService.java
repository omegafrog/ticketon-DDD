package org.codenbug.broker.app;

public interface EntryDispatcherService {
	SSEEntryDispatchService.DispatchResult handle(String userId, String eventId);
}
