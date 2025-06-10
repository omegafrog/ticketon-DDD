package org.codenbug.event.domain;

import java.time.LocalDateTime;

import jakarta.persistence.Embedded;

public class MetaData {
	private Boolean deleted = false;
	private LocalDateTime createdAt;
	private LocalDateTime modifiedAt;
	@Embedded
	private SeatLayoutId seatLayoutId;
}
