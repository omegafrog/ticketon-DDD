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

	@OneToMany(mappedBy = "purchase", cascade = CascadeType.REMOVE, orphanRemoval = true)
	private List<Ticket> tickets = new ArrayList<>();

	protected Purchase(){}

	public Purchase( String eventId, String orderId, int amount ,  UserId userid){
		this.purchaseId = new PurchaseId(Util.ID.createUUID());
		this.orderId = orderId;
		this.eventId = eventId;
		this.amount = amount;
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
}
