package org.codenbug.purchase.infra;

import java.util.Set;
import java.util.stream.Collectors;
import org.codenbug.message.SeatLayoutCreatedEvent;
import org.codenbug.purchase.query.model.Seat;
import org.codenbug.purchase.query.model.SeatLayoutProjection;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class SeatLayoutProjectionConsumer {

    private final JpaSeatLayoutProjectionRepository seatLayoutProjectionRepository;

    public SeatLayoutProjectionConsumer(
        JpaSeatLayoutProjectionRepository seatLayoutProjectionRepository) {
        this.seatLayoutProjectionRepository = seatLayoutProjectionRepository;
    }

    //@KafkaListener(topics = SeatLayoutCreatedEvent.TOPIC, groupId = "purchase-seat-layout-projection-group")
    @Transactional
    public void handleSeatLayoutCreated(SeatLayoutCreatedEvent event) {
        Set<Seat> seats = event.getSeats().stream()
            .map(seatInfo -> new Seat(
                seatInfo.getSeatId(),
                seatInfo.getSignature(),
                seatInfo.getAmount(),
                seatInfo.getGrade()
            ))
            .collect(Collectors.toSet());

        SeatLayoutProjection projection = new SeatLayoutProjection(
            event.getLayoutId(),
            event.getLayout(),
            event.getLocationName(),
            event.getHallName(),
            seats
        );

        seatLayoutProjectionRepository.save(projection);
    }
}