package org.codenbug.message;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class SeatLayoutCreatedEvent {
    public static final String TOPIC = "seat-layout-created";
    
    private Long layoutId;
    private String layout;
    private String locationName;
    private String hallName;
    private List<SeatInfo> seats;

    @Getter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class SeatInfo {
        private String seatId;
        private String signature;
        private int amount;
        private String grade;
        private boolean available;
    }
}