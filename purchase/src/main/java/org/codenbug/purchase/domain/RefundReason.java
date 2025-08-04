package org.codenbug.purchase.domain;

import java.util.Objects;

import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 환불 사유 Value Object
 */
@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RefundReason {
    
    private String value;
    
    public RefundReason(String value) {
        validateReason(value);
        this.value = value.trim();
    }
    
    private void validateReason(String reason) {
        if (reason == null || reason.trim().isEmpty()) {
            throw new IllegalArgumentException("환불 사유는 필수입니다.");
        }
        if (reason.trim().length() > 500) {
            throw new IllegalArgumentException("환불 사유는 500자를 초과할 수 없습니다.");
        }
    }
    
    public boolean isUserRequested() {
        return value.contains("사용자") || value.contains("고객") || value.contains("개인");
    }
    
    public boolean isManagerRequested() {
        return value.contains("매니저") || value.contains("관리자") || value.contains("운영진");
    }
    
    public boolean isSystemRequested() {
        return value.contains("시스템") || value.contains("자동") || value.contains("공연 취소");
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RefundReason that = (RefundReason) o;
        return Objects.equals(value, that.value);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(value);
    }
    
    @Override
    public String toString() {
        return value;
    }
}