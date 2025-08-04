package org.codenbug.purchase.infra;

import java.util.List;
import java.util.Optional;

import org.codenbug.purchase.domain.Purchase;
import org.codenbug.purchase.domain.PurchaseId;
import org.codenbug.purchase.domain.Refund;
import org.codenbug.purchase.domain.RefundId;
import org.codenbug.purchase.domain.RefundRepository;
import org.codenbug.purchase.domain.RefundStatus;
import org.codenbug.purchase.domain.UserId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import lombok.RequiredArgsConstructor;

/**
 * 환불 Repository 구현체
 */
@Repository
@RequiredArgsConstructor
public class RefundRepositoryImpl implements RefundRepository {

	private final JpaRefundRepository jpaRefundRepository;

	@Override
	public Refund save(Refund refund) {
		return jpaRefundRepository.save(refund);
	}

	@Override
	public Optional<Refund> findById(RefundId refundId) {
		return jpaRefundRepository.findById(refundId);
	}

	@Override
	public List<Refund> findByPurchaseId(String purchaseId) {
		return jpaRefundRepository.findByPurchase_PurchaseId(new PurchaseId(purchaseId));
	}

	@Override
	public Page<Refund> findByPurchaseUserId(UserId userId, Pageable pageable) {
		return jpaRefundRepository.findByPurchaseUserId(userId, pageable);
	}

	@Override
	public List<Refund> findByStatus(RefundStatus status) {
		return jpaRefundRepository.findByStatus(status);
	}

	@Override
	public List<Refund> findByPurchaseAndStatus(Purchase purchase, RefundStatus status) {
		return jpaRefundRepository.findByPurchaseAndStatus(purchase, status);
	}

	@Override
	public boolean existsByPurchaseAndStatus(Purchase purchase, RefundStatus status) {
		return jpaRefundRepository.existsByPurchaseAndStatus(purchase, status);
	}

	@Override
	public void delete(Refund refund) {
		jpaRefundRepository.delete(refund);
	}

	@Override
	public void deleteAll(List<Refund> refunds) {
		jpaRefundRepository.deleteAll(refunds);
	}
}