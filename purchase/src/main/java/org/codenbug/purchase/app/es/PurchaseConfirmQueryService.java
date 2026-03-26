package org.codenbug.purchase.app.es;

import org.codenbug.common.exception.AccessDeniedException;
import org.codenbug.purchase.domain.Purchase;
import org.codenbug.purchase.domain.PurchaseId;
import org.codenbug.purchase.domain.es.PurchaseConfirmStatusProjection;
import org.codenbug.purchase.global.ConfirmPaymentStatusResponse;
import org.codenbug.purchase.infra.PurchaseRepository;
import org.codenbug.purchase.infra.es.JpaPurchaseConfirmStatusProjectionRepository;
import org.springframework.stereotype.Service;

@Service
public class PurchaseConfirmQueryService {
	private final PurchaseRepository purchaseRepository;
	private final JpaPurchaseConfirmStatusProjectionRepository projectionRepository;

	public PurchaseConfirmQueryService(PurchaseRepository purchaseRepository,
		JpaPurchaseConfirmStatusProjectionRepository projectionRepository) {
		this.purchaseRepository = purchaseRepository;
		this.projectionRepository = projectionRepository;
	}

	public ConfirmPaymentStatusResponse getStatus(String purchaseId, String userId) {
		Purchase purchase = purchaseRepository.findById(new PurchaseId(purchaseId))
			.orElseThrow(() -> new IllegalArgumentException("구매 정보를 찾을 수 없습니다."));
		if (!purchase.getUserId().getValue().equals(userId)) {
			throw new AccessDeniedException("해당 구매 정보에 대한 접근 권한이 없습니다.");
		}

		PurchaseConfirmStatusProjection proj = projectionRepository.findById(purchaseId)
			.orElseThrow(() -> new IllegalArgumentException("결제 승인 처리 상태를 찾을 수 없습니다."));

		return new ConfirmPaymentStatusResponse(
			purchaseId,
			proj.getStatus().name(),
			proj.getMessage(),
			proj.getUpdatedAt()
		);
	}
}
