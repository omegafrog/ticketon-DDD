package org.codenbug.purchase.domain;

import java.util.Objects;

import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 환불 금액 Value Object
 */
@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RefundAmount {
    
    private Integer value;
    
    public RefundAmount(Integer value) {
        validateAmount(value);
        this.value = value;
    }
    
    private void validateAmount(Integer amount) {
        if (amount == null) {
            throw new IllegalArgumentException("환불 금액은 필수입니다.");
        }
        if (amount <= 0) {
            throw new IllegalArgumentException("환불 금액은 0보다 커야 합니다.");
        }
        if (amount > 10_000_000) {
            throw new IllegalArgumentException("환불 금액은 천만원을 초과할 수 없습니다.");
        }
    }
    
    public boolean isGreaterThan(RefundAmount other) {
        return this.value > other.value;
    }
    
    public boolean isLessThanOrEqualTo(RefundAmount other) {
        return this.value <= other.value;
    }
    
    public RefundAmount add(RefundAmount other) {
        return new RefundAmount(this.value + other.value);
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RefundAmount that = (RefundAmount) o;
        return Objects.equals(value, that.value);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(value);
    }
    
    @Override
    public String toString() {
        return String.format("%,d원", value);
    }
}