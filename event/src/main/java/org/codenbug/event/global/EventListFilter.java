package org.codenbug.event.global;

import java.time.LocalDateTime;
import java.util.List;

import org.codenbug.categoryid.domain.EventCategory;
import org.codenbug.event.domain.EventStatus;
import org.codenbug.seat.domain.Location;
import org.codenbug.seat.domain.RegionLocation;

import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.Getter;

@Getter
public class EventListFilter {
	private CostRange costRange;
	private List<String> locationList;
	private List<RegionLocation> regionLocationList; // RegionLocation enum 지원
	private List<Long> eventCategoryList;
	private List<EventStatus> eventStatusList;
	private LocalDateTime startDate;
	private LocalDateTime endDate;
	private Long categoryId; // 단일 카테고리 필터링 지원

	private EventListFilter(Builder builder) {
		this.costRange = builder.costRange;
		this.locationList = builder.locationList != null ? 
			builder.locationList.stream().map(lo -> lo.getLocationName()).toList() : null;
		this.regionLocationList = builder.regionLocationList;
		this.eventCategoryList = builder.eventCategoryList != null ? 
			builder.eventCategoryList.stream().map(cat -> cat.getId().getId()).toList() : null;
		this.eventStatusList = builder.eventStatusList;
		this.startDate = builder.startDate;
		this.endDate = builder.endDate;
		this.categoryId = builder.categoryId;
	}

	protected EventListFilter() {
	}

	public boolean canFiltered() {
		return costRange != null
			|| (locationList != null && !locationList.isEmpty())
			|| (regionLocationList != null && !regionLocationList.isEmpty())
			|| (eventCategoryList != null && !eventCategoryList.isEmpty())
			|| (eventStatusList != null && !eventStatusList.isEmpty())
			|| startDate != null
			|| endDate != null
			|| categoryId != null;
	}

	public static class Builder {
		private CostRange costRange;
		private List<Location> locationList;
		private List<RegionLocation> regionLocationList;
		private List<EventCategory> eventCategoryList;
		private List<EventStatus> eventStatusList;
		private LocalDateTime startDate;
		private LocalDateTime endDate;
		private Long categoryId;

		@JsonIgnore
		public Builder costRange(CostRange costRange) {
			this.costRange = costRange;
			return this;
		}

		@JsonIgnore
		public Builder locationList(List<Location> locationList) {
			this.locationList = locationList;
			return this;
		}

		@JsonIgnore
		public Builder regionLocationList(List<RegionLocation> regionLocationList) {
			this.regionLocationList = regionLocationList;
			return this;
		}

		@JsonIgnore
		public Builder eventCategoryList(List<EventCategory> eventCategoryList) {
			this.eventCategoryList = eventCategoryList;
			return this;
		}

		@JsonIgnore
		public Builder eventStatusList(List<EventStatus> eventStatusList) {
			this.eventStatusList = eventStatusList;
			return this;
		}

		@JsonIgnore
		public Builder startDate(LocalDateTime startDate) {
			this.startDate = startDate;
			return this;
		}

		@JsonIgnore
		public Builder endDate(LocalDateTime endDate) {
			this.endDate = endDate;
			return this;
		}

		@JsonIgnore
		public Builder categoryId(Long categoryId) {
			this.categoryId = categoryId;
			return this;
		}

		public EventListFilter build() {
			return new EventListFilter(this);
		}

	}

}
