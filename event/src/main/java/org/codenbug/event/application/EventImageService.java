package org.codenbug.event.application;

import java.io.IOException;

import org.codenbug.event.domain.Event;
import org.codenbug.event.domain.EventId;
import org.codenbug.event.domain.EventInformation;
import org.codenbug.event.domain.EventRepository;
import org.codenbug.event.domain.ManagerId;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EventImageService {

	private final EventRepository eventRepository;
	private final ImageStoragePort imageStoragePort;
	private final FileProcessingService fileProcessingService;

	public EventImageService(EventRepository eventRepository, ImageStoragePort imageStoragePort,
			FileProcessingService fileProcessingService) {
		this.eventRepository = eventRepository;
		this.imageStoragePort = imageStoragePort;
		this.fileProcessingService = fileProcessingService;
	}

	@Transactional
	public String replaceImage(EventId eventId, ManagerId managerId, byte[] newImageData, String newFileName)
			throws IOException {
		Event event = eventRepository.findEventForUpdate(eventId);
		event.canUpdate(managerId);

		String previousImageUrl = event.getEventInformation().getThumbnailUrl();
		String previousFileName = fileProcessingService.extractFileNameFromUrl(previousImageUrl);

		String newStaticPath = imageStoragePort.store(newImageData, newFileName);
		imageStoragePort.delete(previousFileName);

		EventInformation changedInformation = event.getEventInformation().replaceThumbnail(newStaticPath);
		event.update(changedInformation);
		eventRepository.save(event);
		return newStaticPath;
	}
}
