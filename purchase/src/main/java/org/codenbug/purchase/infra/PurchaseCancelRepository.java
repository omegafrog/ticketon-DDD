package org.codenbug.purchase.infra;

import org.codenbug.purchase.domain.PurchaseCancel;
import org.codenbug.purchase.domain.port.PurchaseCancelStore;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PurchaseCancelRepository extends JpaRepository<PurchaseCancel, Long>, PurchaseCancelStore {
}
