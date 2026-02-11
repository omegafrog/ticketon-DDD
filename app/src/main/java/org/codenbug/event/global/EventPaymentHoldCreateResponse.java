package org.codenbug.event.global;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class EventPaymentHoldCreateResponse {
	private String holdToken;
	private LocalDateTime expiresAt;
	private Long salesVersion;
}
