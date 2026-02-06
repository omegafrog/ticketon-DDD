package org.codenbug.broker.infra;

import java.util.List;

import org.codenbug.broker.app.EntryDispatcherService;
import org.codenbug.broker.app.SSEEntryDispatchService;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
public class EntryDispatchServiceFacade {

	private final List<String> profiles;
	private final EntryDispatcherService entryDispatchService;
	public EntryDispatchServiceFacade(Environment environment, EntryDispatcherService entryDispatchService) {
		this.entryDispatchService = entryDispatchService;
		String[] activeProfiles = environment.getActiveProfiles();
		this.profiles = List.of(activeProfiles);
	}

	public SSEEntryDispatchService.DispatchResult handle(String userId, String eventId){
		return entryDispatchService.handle(userId, eventId);
	}
}
