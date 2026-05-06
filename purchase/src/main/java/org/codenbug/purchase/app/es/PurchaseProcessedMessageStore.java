package org.codenbug.purchase.app.es;

import org.codenbug.purchase.domain.es.PurchaseProcessedMessage;

public interface PurchaseProcessedMessageStore {
	PurchaseProcessedMessage save(PurchaseProcessedMessage message);
}
