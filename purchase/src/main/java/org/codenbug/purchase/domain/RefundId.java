package org.codenbug.purchase.domain;

import java.util.Objects;

import org.codenbug.common.Util;

import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 환불 ID Value Object
 */
@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RefundId {
    
    private String value;
    
    public RefundId(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("환불 ID는 필수입니다.");
        }
        this.value = value;
    }
    
    public static RefundId generate() {
        return new RefundId("REFUND-" + Util.ID.createUUID());
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RefundId refundId = (RefundId) o;
        return Objects.equals(value, refundId.value);
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