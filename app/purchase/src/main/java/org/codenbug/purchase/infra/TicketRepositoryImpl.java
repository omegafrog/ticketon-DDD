package org.codenbug.purchase.infra;

import java.util.List;

import org.codenbug.purchase.domain.Ticket;
import org.codenbug.purchase.domain.TicketRepository;
import org.springframework.stereotype.Repository;

@Repository
public class TicketRepositoryImpl implements TicketRepository {
	private final JpaTicketRepository repository;

	public TicketRepositoryImpl(JpaTicketRepository repository) {
		this.repository = repository;
	}

	@Override
	public void saveAll(List<Ticket> tickets) {
		repository.saveAll(tickets);
	}
}
