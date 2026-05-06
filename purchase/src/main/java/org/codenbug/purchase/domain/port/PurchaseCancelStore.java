package org.codenbug.purchase.domain.port;

import org.codenbug.purchase.domain.PurchaseCancel;

public interface PurchaseCancelStore {
	PurchaseCancel save(PurchaseCancel purchaseCancel);
}
