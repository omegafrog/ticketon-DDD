package org.codenbug.broker.domain;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import lombok.Setter;

public class SseConnection {
	private String userId;
	private SseEmitter emitter;
	@Setter
	private Status status;

	private String eventId;

	public SseConnection() {
	}

	public SseConnection(String userId, SseEmitter emitter, Status status, String eventId) {
		this.userId = userId;
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

	public String getUserId() {
		return userId;
	}
}
