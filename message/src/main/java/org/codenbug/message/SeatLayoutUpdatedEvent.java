package org.codenbug.message;

public class SeatLayoutUpdatedEvent {
    private final Long seatLayoutId;

    public SeatLayoutUpdatedEvent(Long seatLayoutId) {
        this.seatLayoutId = seatLayoutId;
    }

    public Long getSeatLayoutId() {
        return seatLayoutId;
    }
}