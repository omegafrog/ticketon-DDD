package org.codenbug.purchase.app.es;

import java.util.Optional;

import org.codenbug.purchase.domain.PurchaseId;
import org.codenbug.purchase.domain.es.PurchaseConfirmStatusProjection;

public interface PurchaseConfirmStatusProjectionStore {
  Optional<PurchaseConfirmStatusProjection> findById(String purchaseId);

  Optional<PurchaseConfirmStatusProjection> findById(PurchaseId purchaseId);

  PurchaseConfirmStatusProjection save(PurchaseConfirmStatusProjection projection);
}
