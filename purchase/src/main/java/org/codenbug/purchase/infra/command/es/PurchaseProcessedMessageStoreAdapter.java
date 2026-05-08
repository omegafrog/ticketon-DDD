package org.codenbug.purchase.infra.command.es;

import org.codenbug.purchase.infra.es.JpaPurchaseProcessedMessageRepository;
import org.codenbug.purchase.domain.port.es.PurchaseProcessedMessageStore;
import org.codenbug.purchase.domain.es.PurchaseProcessedMessage;
import org.springframework.stereotype.Component;

@Component
class PurchaseProcessedMessageStoreAdapter implements PurchaseProcessedMessageStore {
	private final JpaPurchaseProcessedMessageRepository repository;

	PurchaseProcessedMessageStoreAdapter(JpaPurchaseProcessedMessageRepository repository) {
		this.repository = repository;
	}

	@Override
	public PurchaseProcessedMessage save(PurchaseProcessedMessage message) {
		return repository.saveAndFlush(message);
	}

	@Override
	public void deleteById(String messageId) {
		repository.deleteById(messageId);
	}
}
