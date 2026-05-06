package org.codenbug.purchase.infra.es;

import org.codenbug.purchase.app.es.PurchaseProcessedMessageStore;
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
		return repository.save(message);
	}
}
