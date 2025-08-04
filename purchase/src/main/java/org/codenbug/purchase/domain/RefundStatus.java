package org.codenbug.purchase.domain;

/**
 * 환불 상태 열거형
 */
public enum RefundStatus {
    REQUESTED("환불 요청"),
    PROCESSING("환불 처리 중"),
    COMPLETED("환불 완료"),
    FAILED("환불 실패"),
    PARTIAL_REFUNDED("부분 환불 완료");
    
    private final String description;
    
    RefundStatus(String description) {
        this.description = description;
    }
    
    public String getDescription() {
        return description;
    }
    
    public boolean isCompleted() {
        return this == COMPLETED || this == PARTIAL_REFUNDED;
    }
    
    public boolean isFailed() {
        return this == FAILED;
    }
    
    public boolean canProcess() {
        return this == REQUESTED;
    }
}