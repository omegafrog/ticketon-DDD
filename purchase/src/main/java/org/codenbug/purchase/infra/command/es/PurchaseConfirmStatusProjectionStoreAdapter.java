package org.codenbug.purchase.infra.command.es;

import org.codenbug.purchase.infra.es.JpaPurchaseConfirmStatusProjectionRepository;
import java.util.Optional;

import org.codenbug.purchase.domain.port.es.PurchaseConfirmStatusProjectionStore;
import org.codenbug.purchase.domain.PurchaseId;
import org.codenbug.purchase.domain.es.PurchaseConfirmStatusProjection;
import org.springframework.stereotype.Component;

@Component
class PurchaseConfirmStatusProjectionStoreAdapter implements PurchaseConfirmStatusProjectionStore {
  private final JpaPurchaseConfirmStatusProjectionRepository repository;

  PurchaseConfirmStatusProjectionStoreAdapter(JpaPurchaseConfirmStatusProjectionRepository repository) {
    this.repository = repository;
  }

  @Override
  public Optional<PurchaseConfirmStatusProjection> findById(String purchaseId) {
    return repository.findById(purchaseId);
  }

  @Override
  public Optional<PurchaseConfirmStatusProjection> findById(PurchaseId purchaseId) {
    return repository.findById(purchaseId.getValue());
  }

  @Override
  public PurchaseConfirmStatusProjection save(PurchaseConfirmStatusProjection projection) {
    return repository.save(projection);
  }
}
