package org.codenbug.event.application.dto.request;

import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.codenbug.seat.global.SeatDto;

@Getter
@NoArgsConstructor
public class NewSeatRequest {

    private String hallName;
    private int seatCount;
    private String location;
    private String layout;
    private List<SeatDto> seats;

    public NewSeatRequest(String hallName, String location, String seatLayout,
        List<SeatDto> seats) {
        this.hallName = hallName;
        this.seatCount = seats.size();
        this.location = location;
        this.layout = seatLayout;
        this.seats = seats;
    }
}
