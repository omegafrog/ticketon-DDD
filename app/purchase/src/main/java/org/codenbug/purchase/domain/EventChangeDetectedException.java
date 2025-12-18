package org.codenbug.purchase.domain;

/**
 * Event 상태 변경이 감지되었을 때 발생하는 예외
 * 보상 트랜잭션 처리를 위한 정보를 포함합니다.
 */
public class EventChangeDetectedException extends RuntimeException {
    
    private final String paymentKey;
    private final PrePaymentValidationResult preResult;
    
    public EventChangeDetectedException(String message, String paymentKey, PrePaymentValidationResult preResult) {
        super(message);
        this.paymentKey = paymentKey;
        this.preResult = preResult;
    }
    
    public String getPaymentKey() {
        return paymentKey;
    }
    
    public PrePaymentValidationResult getPreResult() {
        return preResult;
    }
}