package org.codenbug.purchase.infra;

import org.codenbug.purchase.domain.PurchaseCancel;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PurchaseCancelRepository extends JpaRepository<PurchaseCancel, Long> {
}
