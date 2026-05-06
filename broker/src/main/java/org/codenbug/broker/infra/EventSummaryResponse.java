package org.codenbug.broker.infra;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class EventSummaryResponse {

  private String eventId;
  private Long seatLayoutId;
  private boolean seatSelectable;
  private String status;
  private Long version;
  private Long salesVersion;
  private String title;
  private String managerId;

  public EventSummaryResponse(String eventId, Long seatLayoutId, boolean seatSelectable, String status, Long version,
      Long salesVersion, String title) {
    this(eventId, seatLayoutId, seatSelectable, status, version, salesVersion, title, null);
  }

  public EventSummaryResponse(String eventId, Long seatLayoutId, boolean seatSelectable, String status, Long version,
      Long salesVersion, String title, String managerId) {
    this.eventId = eventId;
    this.seatLayoutId = seatLayoutId;
    this.seatSelectable = seatSelectable;
    this.status = status;
    this.version = version;
    this.salesVersion = salesVersion;
    this.title = title;
    this.managerId = managerId;
  }
}
