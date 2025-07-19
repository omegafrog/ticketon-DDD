package org.codenbug.event.global;

import java.time.LocalDateTime;
import java.util.List;

import org.codenbug.categoryid.domain.EventCategory;
import org.codenbug.event.domain.EventStatus;
import org.codenbug.seat.domain.Location;

import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.Getter;

@Getter
public class EventListFilter {
	private CostRange costRange;
	private List<String> locationList;
	private List<Long> eventCategoryList;
	private List<EventStatus> eventStatusList;
	private LocalDateTime startDate;
	private LocalDateTime endDate;

	private EventListFilter(Builder builder) {
		this.costRange = builder.costRange;
		this.locationList = builder.locationList.stream().map(lo -> lo.getLocationName()).toList();
		this.eventCategoryList = builder.eventCategoryList.stream().map(cat -> cat.getId().getId()).toList();
		this.eventStatusList = builder.eventStatusList;
		this.startDate = builder.startDate;
		this.endDate = builder.endDate;
	}

	protected EventListFilter() {
	}

	public boolean canFiltered() {
		return costRange != null
			|| (locationList != null && !locationList.isEmpty())
			|| (eventCategoryList != null && !eventCategoryList.isEmpty()
			|| (eventStatusList != null && !eventStatusList.isEmpty())
			|| startDate != null
			|| endDate != null);
	}

	public static class Builder {
		private CostRange costRange;
		private List<Location> locationList;
		private List<EventCategory> eventCategoryList;
		private List<EventStatus> eventStatusList;
		private LocalDateTime startDate;
		private LocalDateTime endDate;

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

		public EventListFilter build() {
			return new EventListFilter(this);
		}

	}

}
