package org.codenbug.purchase.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.Getter;

@Embeddable
@Getter
public class PurchaseId {
	@Column(name="purchase_id", unique=true)
	private String value;

	protected PurchaseId(){};

	public PurchaseId(String value){
		this.value = value;
	}
}
