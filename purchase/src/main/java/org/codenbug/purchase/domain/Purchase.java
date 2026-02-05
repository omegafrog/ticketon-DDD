package org.codenbug.purchase.domain;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.codenbug.common.Util;
import org.codenbug.common.exception.AccessDeniedException;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.OneToMany;
import lombok.Getter;
import lombok.Setter;

/**
 * Purchase 엔티티 클래스
 */
@Entity
@Getter
@EntityListeners(AuditingEntityListener.class)
public class Purchase {
	@EmbeddedId
	private PurchaseId purchaseId;

	// 클라이언트로부터 생성한 무작위 값
	private String orderId;

	private String orderName;

	private String eventId;

	private String pid;

	private int amount;

	/**
	 * 결제 핵심 필드(payment-relevant fields) 변경 감지를 위한 스냅샷 버전.
	 * - 결제 요청 시점에 Event 서비스의 salesVersion을 캡처
	 */
	@Column(name = "expected_sales_version")
	private Long expectedSalesVersion;

	@Enumerated(EnumType.STRING)
	private PaymentMethod paymentMethod;

	@Setter
	@Enumerated(EnumType.STRING)
	@Column(name = "payment_status", length = 50)
	private PaymentStatus paymentStatus;

	@CreatedDate
	private LocalDateTime createdAt;

	@Embedded
	private UserId userId;

	@OneToMany(mappedBy = "purchase", cascade = CascadeType.REMOVE, orphanRemoval = true, fetch = FetchType.LAZY)
	private List<Ticket> tickets = new ArrayList<>();

	protected Purchase(){}

	public Purchase(String eventId, String orderId, int amount, Long expectedSalesVersion, UserId userid) {
		this.purchaseId = new PurchaseId(Util.ID.createUUID());
		this.orderId = orderId;
		this.eventId = eventId;
		this.amount = amount;
		this.expectedSalesVersion = expectedSalesVersion;
		this.userId = userid;
		this.paymentStatus = PaymentStatus.IN_PROGRESS;
	}

	public void updatePaymentInfo(
		String paymentUuid,
		String eventId,
		int amount,
		PaymentMethod paymentMethod,
		String purchaseName,
		LocalDateTime createdDate
	) {
		this.pid = paymentUuid;
		this.eventId = eventId;
		this.amount = amount;
		this.paymentMethod = paymentMethod;
		this.orderName = purchaseName;
		this.createdAt = createdDate;
	}

	public void addTicket(Ticket ticket) {
		this.tickets.add(ticket);
		ticket.assignToPurchase(this);
	}

	public void addTickets(List<Ticket> tickets) {
		for (Ticket ticket : tickets) {
			addTicket(ticket);
		}
	}

	public void validate(String orderId, Integer amount, String userId) {
		validateOrderId(orderId);
		validateAmount(amount);
		validateUserAccess(userId);
	}

	private void validateOrderId(String orderId) {
		if(!this.orderId.equals(orderId)) {
			throw new IllegalArgumentException("주문 번호가 일치하지 않습니다.");
		}
	}

	private void validateAmount(Integer amount) {
		if(this.amount != amount) {
			throw new IllegalArgumentException("결제 금액이 일치하지 않습니다.");
		}
	}

	private void validateUserAccess(String userId) {
		if(!this.userId.getValue().equals(userId)) {
			throw new AccessDeniedException("해당 구매 정보에 대한 접근 권한이 없습니다.");
		}
	}

	public void markAsCompleted() {
		this.paymentStatus = PaymentStatus.DONE;
	}

	public boolean isPaymentInProgress() {
		return this.paymentStatus == PaymentStatus.IN_PROGRESS;
	}

	public boolean isPaymentCompleted() {
		return this.paymentStatus == PaymentStatus.DONE;
	}
	
	// 환불 관련 비즈니스 로직
	public boolean canRefund() {
		return isPaymentCompleted();
	}
	
	public void validateRefundAmount(Integer refundAmount) {
		if (refundAmount == null || refundAmount <= 0) {
			throw new IllegalArgumentException("환불 금액은 0보다 커야 합니다.");
		}
		if (refundAmount > this.amount) {
			throw new IllegalArgumentException("환불 금액이 구매 금액을 초과할 수 없습니다.");
		}
	}
	
	public void markAsRefunded() {
		if (!canRefund()) {
			throw new IllegalStateException("완료된 결제만 환불할 수 있습니다.");
		}
		this.paymentStatus = PaymentStatus.REFUNDED;
	}
	
	public void markAsPartialRefunded() {
		if (!canRefund()) {
			throw new IllegalStateException("완료된 결제만 부분 환불할 수 있습니다.");
		}
		this.paymentStatus = PaymentStatus.PARTIAL_REFUNDED;
	}
	
	public int getTotalAmount() {
		return this.amount;
	}
	
	public String getPaymentKey() {
		return this.pid;
	}
	
	public void cancel() {
		this.paymentStatus = PaymentStatus.CANCELED;
	}
	
	public void markAsFailed() {
		this.paymentStatus = PaymentStatus.FAILED;
	}
}
