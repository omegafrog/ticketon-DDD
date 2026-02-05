package org.codenbug.purchase.infra.client;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class EventSummaryResponse {
	private String eventId;
	private Long seatLayoutId;
	private boolean seatSelectable;
	private String status;
	private Long version;
	private Long salesVersion;
	private String title;
}
