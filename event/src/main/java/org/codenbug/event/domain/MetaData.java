package org.codenbug.event.domain;

import java.time.LocalDateTime;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;

import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import lombok.Getter;

@Embeddable
@Getter
public class MetaData {
	private Boolean deleted;
	@CreatedDate
	private LocalDateTime createdAt;
	@LastModifiedDate
	private LocalDateTime modifiedAt;
	@Embedded
	private SeatLayoutId seatLayoutId;

	protected MetaData() {}

	public MetaData( SeatLayoutId seatLayoutId) {
		this.deleted = false;
		this.seatLayoutId = seatLayoutId;
		validate();
	}

	protected void validate(){
		if(seatLayoutId == null){
			throw new IllegalStateException("Seat Layout ID is required");
		}
	}
}
