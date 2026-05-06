package org.codenbug.purchase.domain.port.es;

import org.codenbug.purchase.domain.es.PurchaseProcessedMessage;

public interface PurchaseProcessedMessageStore {
	PurchaseProcessedMessage save(PurchaseProcessedMessage message);
}
