package org.codenbug.purchase.infra.client;

import java.time.LocalDateTime;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class EventPaymentHoldCreateResponse {
	private String holdToken;
	private LocalDateTime expiresAt;
	private Long salesVersion;
}
