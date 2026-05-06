package org.codenbug.purchase.app;

import org.codenbug.purchase.domain.PurchaseCancel;

public interface PurchaseCancelStore {
	PurchaseCancel save(PurchaseCancel purchaseCancel);
}
