package org.codenbug.purchase.app.query.es;

import org.codenbug.purchase.domain.port.es.PurchaseConfirmStatusProjectionStore;
import org.codenbug.common.exception.AccessDeniedException;
import org.codenbug.purchase.domain.Purchase;
import org.codenbug.purchase.domain.PurchaseId;
import org.codenbug.purchase.domain.es.PurchaseConfirmStatusProjection;
import org.codenbug.purchase.ui.response.ConfirmPaymentStatusResponse;
import org.codenbug.purchase.domain.port.PurchaseRepository;
import org.springframework.stereotype.Service;

@Service
public class PurchaseConfirmQueryService {
  private final PurchaseRepository purchaseRepository;
  private final PurchaseConfirmStatusProjectionStore projectionRepository;

  public PurchaseConfirmQueryService(PurchaseRepository purchaseRepository,
      PurchaseConfirmStatusProjectionStore projectionRepository) {
    this.purchaseRepository = purchaseRepository;
    this.projectionRepository = projectionRepository;
  }

  public ConfirmPaymentStatusResponse getStatus(String id, String userId) {
    PurchaseId purchaseId = new PurchaseId(id);
    Purchase purchase = purchaseRepository.findById(purchaseId)
        .orElseThrow(() -> new IllegalArgumentException("구매 정보를 찾을 수 없습니다."));
    if (!purchase.getUserId().getValue().equals(userId)) {
      throw new AccessDeniedException("해당 구매 정보에 대한 접근 권한이 없습니다.");
    }

    PurchaseConfirmStatusProjection proj = projectionRepository.findById(purchaseId)
        .orElseThrow(() -> new IllegalArgumentException("결제 승인 처리 상태를 찾을 수 없습니다."));

    return new ConfirmPaymentStatusResponse(
        purchaseId.getValue(),
        proj.getStatus().name(),
        purchase.getPaymentStatus().name(),
        proj.getMessage(),
        proj.getUpdatedAt());
  }
}
