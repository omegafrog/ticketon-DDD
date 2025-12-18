package org.codenbug.purchase.domain;

import java.util.List;

public interface TicketRepository {
	void saveAll(List<Ticket> tickets);
}
