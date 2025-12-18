package org.codenbug.event.domain;

import java.time.LocalDateTime;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;

import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Embeddable
@Getter
@AllArgsConstructor
public class MetaData {
	private Boolean deleted;
	@CreatedDate
	private LocalDateTime createdAt;
	@LastModifiedDate
	private LocalDateTime modifiedAt;

	public MetaData() {
		this.deleted = false;
		validate();
	}

	public MetaData ofDeleted() {
		return new MetaData(true, this.createdAt, LocalDateTime.now());
	}

	protected void validate() {

	}

}
