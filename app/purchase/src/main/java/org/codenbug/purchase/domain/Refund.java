package org.codenbug.purchase.domain;

import java.time.LocalDateTime;
import java.util.Objects;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 환불 Aggregate Root
 */
@Entity
@Table(name = "refund")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class Refund {
    
    @EmbeddedId
    private RefundId refundId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "purchase_id", nullable = false)
    private Purchase purchase;
    
    @Embedded
    @AttributeOverride(name = "value", column = @Column(name = "refund_amount"))
    private RefundAmount refundAmount;
    
    @Embedded
    @AttributeOverride(name = "value", column = @Column(name = "refund_reason", length = 500))
    private RefundReason refundReason;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RefundStatus status;
    
    @Column(name = "receipt_url")
    private String receiptUrl;
    
    @Column(name = "pg_transaction_id")
    private String pgTransactionId;
    
    @Embedded
    private UserId processedBy;
    
    @CreatedDate
    @Column(name = "requested_at", nullable = false, updatable = false)
    private LocalDateTime requestedAt;
    
    @Column(name = "processed_at")
    private LocalDateTime processedAt;
    
    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    // 생성자
    public Refund(Purchase purchase, RefundAmount refundAmount, RefundReason refundReason, UserId processedBy) {
        this.refundId = RefundId.generate();
        this.purchase = Objects.requireNonNull(purchase, "Purchase는 필수입니다.");
        this.refundAmount = Objects.requireNonNull(refundAmount, "환불 금액은 필수입니다.");
        this.refundReason = Objects.requireNonNull(refundReason, "환불 사유는 필수입니다.");
        this.processedBy = processedBy;
        this.status = RefundStatus.REQUESTED;
        
        validateRefundAmount(purchase, refundAmount);
    }
    
    // 팩토리 메서드
    public static Refund createUserRefund(Purchase purchase, Integer amount, String reason, UserId userId) {
        return new Refund(
            purchase,
            new RefundAmount(amount),
            new RefundReason(reason),
            userId
        );
    }
    
    public static Refund createManagerRefund(Purchase purchase, Integer amount, String reason, UserId managerId) {
        RefundReason managerReason = new RefundReason("매니저 처리: " + reason);
        return new Refund(
            purchase,
            new RefundAmount(amount),
            managerReason,
            managerId
        );
    }
    
    public static Refund createSystemRefund(Purchase purchase, Integer amount, String reason) {
        RefundReason systemReason = new RefundReason("시스템 자동 처리: " + reason);
        return new Refund(
            purchase,
            new RefundAmount(amount),
            systemReason,
            null // 시스템 처리는 processedBy가 null
        );
    }
    
    // 비즈니스 로직
    public void startProcessing() {
        if (!status.canProcess()) {
            throw new IllegalStateException("환불 요청 상태에서만 처리를 시작할 수 있습니다.");
        }
        this.status = RefundStatus.PROCESSING;
    }
    
    public void completeRefund(String pgTransactionId, String receiptUrl) {
        if (this.status != RefundStatus.PROCESSING) {
            throw new IllegalStateException("처리 중인 환불만 완료할 수 있습니다.");
        }
        
        this.status = RefundStatus.COMPLETED;
        this.pgTransactionId = pgTransactionId;
        this.receiptUrl = receiptUrl;
        this.processedAt = LocalDateTime.now();
    }
    
    public void failRefund(String reason) {
        this.status = RefundStatus.FAILED;
        this.refundReason = new RefundReason(this.refundReason.getValue() + " (실패 사유: " + reason + ")");
        this.processedAt = LocalDateTime.now();
    }
    
    public void markAsPartialRefund() {
        this.status = RefundStatus.PARTIAL_REFUNDED;
        this.processedAt = LocalDateTime.now();
    }
    
    // 검증 로직
    private void validateRefundAmount(Purchase purchase, RefundAmount refundAmount) {
        if (refundAmount.getValue() > purchase.getAmount()) {
            throw new IllegalArgumentException("환불 금액이 구매 금액을 초과할 수 없습니다.");
        }
    }
    
    public void validateUserAccess(UserId userId) {
        if (!this.purchase.getUserId().equals(userId)) {
            throw new IllegalArgumentException("해당 환불에 대한 접근 권한이 없습니다.");
        }
    }
    
    // 조회 메서드
    public boolean isCompleted() {
        return status.isCompleted();
    }
    
    public boolean isFailed() {
        return status.isFailed();
    }
    
    public boolean canCancel() {
        return status == RefundStatus.REQUESTED;
    }
    
    public boolean isUserRefund() {
        return refundReason.isUserRequested();
    }
    
    public boolean isManagerRefund() {
        return refundReason.isManagerRequested();
    }
    
    public boolean isSystemRefund() {
        return refundReason.isSystemRequested();
    }
    
    public String getRefundAmountValue() {
        return refundAmount.getValue().toString();
    }
    
    public String getRefundReasonValue() {
        return refundReason.getValue();
    }
}