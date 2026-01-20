package org.codenbug.purchase.infra.client;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class SeatDto {
	private String id;
	private String signature;
	private String grade;
	private int price;
	private boolean available;
}
