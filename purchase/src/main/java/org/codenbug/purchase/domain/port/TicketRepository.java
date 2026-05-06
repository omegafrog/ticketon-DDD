package org.codenbug.purchase.domain.port;

import org.codenbug.purchase.domain.Ticket;
import java.util.List;

public interface TicketRepository {
	void saveAll(List<Ticket> tickets);
}
