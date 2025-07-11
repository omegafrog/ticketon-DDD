package org.codenbug.broker.entity;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import lombok.Setter;

public class SseConnection {

	private SseEmitter emitter;
	@Setter
	private Status status;

	private String eventId;

	public SseConnection() {
	}

	public SseConnection(SseEmitter emitter, Status status, String eventId) {
		this.emitter = emitter;
		this.status = status;
		this.eventId = eventId;
	}

	public SseEmitter getEmitter() {
		return emitter;
	}

	public Status getStatus() {
		return status;
	}

	public String getEventId() {
		return eventId;
	}
}
