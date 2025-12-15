package org.codenbug.seat.global;

import java.util.List;

import org.codenbug.seat.domain.RegionLocation;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

@Getter
@Valid
public class RegisterSeatLayoutDto {
	private List<List<String>> layout;
	private List<SeatDto> seats;
	@NotBlank
	private String location;
	@NotBlank
	private String hallName;
	private RegionLocation regionLocation;

	protected RegisterSeatLayoutDto() {}

	public RegisterSeatLayoutDto(List<List<String>> layout, List<SeatDto> seats, String location,
			String hallName) {
		this.layout = layout;
		this.seats = seats;
		this.location = location;
		this.hallName = hallName;
	}

	public RegisterSeatLayoutDto(List<List<String>> layout, List<SeatDto> seats, String location,
			String hallName, RegionLocation regionLocation) {
		this.layout = layout;
		this.seats = seats;
		this.location = location;
		this.hallName = hallName;
		this.regionLocation = regionLocation;
	}


}
