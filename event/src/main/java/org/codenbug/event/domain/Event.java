package org.codenbug.event.domain;

import java.time.LocalDateTime;

import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import jakarta.persistence.Embedded;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import lombok.Getter;

/**
 * 이벤트의 정보를 담고 있는 루트 애그리거트 클래스.
 * 비즈니스 로직에 사용되면서 유저에게 노출되는 데이터는 {@link EventInformation}, 그렇지 않은 데이터는 {@link MetaData}에 포함됨
 * {@link EventId}는 UUIDv7이 적용됨
 */
@Entity
@Getter
@EntityListeners(AuditingEntityListener.class)
public class Event {

	@EmbeddedId
	private EventId eventId;

	@Embedded
	private EventInformation eventInformation;
	@Embedded
	private Manager manager;
	@Embedded
	private SeatLayoutId seatLayoutId;
	@Embedded
	private MetaData metaData;

	protected Event() {}

	public Event( EventInformation information, SeatLayoutId seatLayoutId, Manager manager, MetaData metaData) {
		information.validate();
		metaData.validate();
		this.eventId = generateEventId();
		this.eventInformation = information;
		this.metaData = metaData;
		this.manager = manager;
	}

	private static EventId generateEventId() {
		throw new RuntimeException("eventId를 생성");
	}

	/**
	 * event의 정보를 수정하는 메서드
	 * 수정 시각이 예매가 끝난 이후라면 수정에 실패한다
	 * @param information
	 * @param metaData
	 */
	public void update( EventInformation information, MetaData metaData ) {
		verifyBookingNotEnded();
		information.validate();
		metaData.validate();
		this.eventInformation = information;
		this.metaData = metaData;
	}

	private void verifyBookingNotEnded() {
		if(LocalDateTime.now().isAfter(this.eventInformation.getBookingEnd()))
			throw new IllegalStateException("Cannot update event after booking is end");
	}

}
