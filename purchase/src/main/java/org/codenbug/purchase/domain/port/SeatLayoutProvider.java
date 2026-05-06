package org.codenbug.purchase.domain.port;

import org.codenbug.purchase.domain.SeatLayoutInfo;
public interface SeatLayoutProvider {
	SeatLayoutInfo getSeatLayout(Long seatLayoutId);
}
