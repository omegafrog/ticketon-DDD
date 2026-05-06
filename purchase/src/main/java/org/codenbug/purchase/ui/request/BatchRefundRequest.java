package org.codenbug.purchase.ui.request;

import jakarta.validation.constraints.NotBlank;

public class BatchRefundRequest {
	@NotBlank
	private String eventId;
	@NotBlank
	private String refundReason;
	@NotBlank
	private String managerId;
	@NotBlank
	private String managerName;

	public String getEventId() {
		return eventId;
	}

	public void setEventId(String eventId) {
		this.eventId = eventId;
	}

	public String getRefundReason() {
		return refundReason;
	}

	public void setRefundReason(String refundReason) {
		this.refundReason = refundReason;
	}

	public String getManagerId() {
		return managerId;
	}

	public void setManagerId(String managerId) {
		this.managerId = managerId;
	}

	public String getManagerName() {
		return managerName;
	}

	public void setManagerName(String managerName) {
		this.managerName = managerName;
	}
}
