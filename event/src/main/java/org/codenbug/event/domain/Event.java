package org.codenbug.event.domain;

import jakarta.persistence.Embedded;
import jakarta.persistence.EmbeddedId;

/**
 * 이벤트의 정보를 담고 있는 루트 애그리거트 클래스.
 * 비즈니스 로직에 사용되면서 유저에게 노출되는 데이터는 {@link EventInformation}, 그렇지 않은 데이터는 {@link MetaData}에 포함됨
 * {@link EventId}는 UUIDv7이 적용됨
 */
public class Event {

	@EmbeddedId
	private EventId eventId;

	@Embedded
	private EventInformation information;

	@Embedded
	private MetaData metaData;

	protected Event() {}

	public Event( EventInformation information, MetaData metaData) {
		this.eventId = generateEventId();
		information.validate();
		metaData.validate();
		this.information = information;
		this.metaData = metaData;
	}

	private static EventId generateEventId() {
		throw new RuntimeException("eventId를 생성");
	}
}
