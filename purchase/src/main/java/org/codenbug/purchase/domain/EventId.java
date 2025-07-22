package org.codenbug.purchase.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.Getter;

@Embeddable
@Getter
public class EventId {
	@Column(name = "event_id")
	private String value;

	protected EventId(){}
	public EventId(String value){
		this.value = value;
	}
}
