package org.codenbug.purchase.domain;

public enum PaymentStatus {
	IN_PROGRESS("진행 중"),
	DONE("완료"),
	CANCELED("취소"),
	EXPIRED("만료"),
	REFUNDED("환불 완료"),
	PARTIAL_REFUNDED("부분 환불");
	
	private final String description;
	
	PaymentStatus(String description) {
		this.description = description;
	}
	
	public String getDescription() {
		return description;
	}
	
	public boolean isRefunded() {
		return this == REFUNDED || this == PARTIAL_REFUNDED;
	}
	
	public boolean canRefund() {
		return this == DONE;
	}
}
