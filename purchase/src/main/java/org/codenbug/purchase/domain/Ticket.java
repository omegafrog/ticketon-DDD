package org.codenbug.purchase.domain;

import java.time.LocalDateTime;

import org.codenbug.common.Util;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import jakarta.persistence.Embedded;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.ManyToOne;
import lombok.Getter;

@Entity
@Getter
@EntityListeners(AuditingEntityListener.class)
public class Ticket {
	@EmbeddedId
	private TicketId id;
	private String location;
	@CreatedDate
	private LocalDateTime purchaseDate;

	@Embedded
	private EventId eventId;

	private String seatId;


	@ManyToOne
	private Purchase purchase;

	protected Ticket(){}
	public Ticket(String location, EventId eventId, String seatId, Purchase purchase){
		this.seatId = seatId;
		this.id = new TicketId(Util.ID.createUUID());
		this.location = location;
		this.eventId = eventId;
		this.purchase = purchase;
		this.purchase.setTicket(this);
	}

}
