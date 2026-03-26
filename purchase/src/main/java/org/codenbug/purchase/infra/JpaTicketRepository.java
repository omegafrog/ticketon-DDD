package org.codenbug.purchase.infra;

import org.codenbug.purchase.domain.Ticket;
import org.codenbug.purchase.domain.TicketId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaTicketRepository extends JpaRepository<Ticket, TicketId> {
}
