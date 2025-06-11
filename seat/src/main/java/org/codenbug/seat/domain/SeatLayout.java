package org.codenbug.seat.domain;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

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
@Table(name="seat_layout")
public class SeatLayout {
	@Id
	@GeneratedValue(strategy= GenerationType.IDENTITY)
	private Long id;

	@Column(name = "layout")
	private String layout;

	@ElementCollection
	@CollectionTable(name = "seat",
	joinColumns = @JoinColumn(name = "layout_id"))
	private Set<Seat> seats;


	protected SeatLayout() {}

	public SeatLayout(String layout, List<Seat> seats){
		this.layout = layout;
		this.seats = new HashSet<>(seats);
	}
	protected void validate(){
		if(this.layout == null){
			throw new RuntimeException("layout cannot be null");
		}
	}
}
