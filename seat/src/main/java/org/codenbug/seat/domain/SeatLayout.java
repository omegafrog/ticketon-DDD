package org.codenbug.seat.domain;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.codenbug.seat.global.SeatDto;
import org.codenbug.seat.global.UpdateSeatLayoutRequest;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import lombok.Getter;

@Getter
@Entity
@Table(name = "seat_layout")
public class SeatLayout {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "layout")
	private String layout;

	@ElementCollection
	@CollectionTable(name = "seat",
		joinColumns = @JoinColumn(name = "layout_id"))
	private Set<Seat> seats;

	protected SeatLayout() {
	}

	public SeatLayout(List<List<String>> layout, List<Seat> seats) {
		validate(layout, seats);
		this.layout = convertLayout(layout);
		this.seats = new HashSet<>(seats);
	}

	private String convertLayout(List<List<String>> layout) {
		StringBuilder builder = new StringBuilder();
		builder.append("[\n");
		for (List<String> row : layout) {
			builder.append("[");
			builder.append(row.stream().collect(Collectors.joining(", ")));
			builder.append("]");
			builder.append(",\n");
		}
		builder.append("]");
		return builder.toString();
	}

	private void validate(List<List<String>> layout, List<Seat> seats) {
		// 	"layout":[
		// 	["A1", "A2", "A3"],
		// 	["B1", "B2", "B3"]
		// ],
		// "seats":[
		// 	{
		// 		"signature":"A1",
		// 		"grade": "S",
		// 		"price": 123
		// 	}
		// ]
		// layout has signature, so seats list item's signature must equal to layout's signature
		// Check if all signatures in 'seats' are present in 'layout'
		long size = layout.stream().flatMap(List::stream).filter(s -> s!=null).count();
		if (seats.size() != size)
			throw new IllegalArgumentException("layout's signature must match with seats");
		for (Seat seat : seats) {
			boolean found = false;
			for (List<String> row : layout) {
				if (row.contains(seat.getSignature())) {
					found = true;
					break;
				}
			}
			if (!found) {
				throw new IllegalArgumentException("Seat signature '" + seat.getSignature() + "' not found in layout.");
			}
		}

		// Check if all signatures in 'layout' are present in 'seats'
		for (List<String> row : layout) {
			for (String signature : row) {
				if(signature == null)
					continue;
				boolean found = false;
				for (Seat seat : seats) {
					if (seat.getSignature().equals(signature)) {
						found = true;
						break;
					}
				}
				if (!found) {
					throw new IllegalArgumentException("Layout signature '" + signature + "' not found in seats.");
				}
			}
		}

	}

	public void update(List<List<String>> layout, List<Seat> seats) {
		this.layout = convertLayout(layout);
		this.seats = new HashSet<>(seats);
	}
}
