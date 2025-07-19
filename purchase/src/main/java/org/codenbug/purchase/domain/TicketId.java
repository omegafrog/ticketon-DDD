package org.codenbug.purchase.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.Getter;

@Embeddable
@Getter
public class TicketId {
	@Column(name="ticket_id", unique=true)
	private String value;

	protected TicketId(){}
	public TicketId(String value){
		this.value = value;
	}

}
