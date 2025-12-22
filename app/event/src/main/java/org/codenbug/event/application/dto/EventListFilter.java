package org.codenbug.event.application.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.Valid;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import lombok.Getter;
import org.codenbug.categoryid.domain.EventCategory;
import org.codenbug.event.domain.EventStatus;
import org.codenbug.seat.domain.Location;
import org.codenbug.seat.domain.RegionLocation;

@Getter
public class EventListFilter {

    @Valid
    private CostRange costRange;
    private List<@NotBlank(message = "지역 이름은 공백일 수 없습니다.") String> locationList;
    private List<@NotNull(message = "지역 Enum 값은 null일 수 없습니다.") RegionLocation> regionLocationList; // RegionLocation enum 지원
    private List<@NotNull @Positive(message = "카테고리 ID는 양수여야 합니다.") Long> eventCategoryList;
    private List<@NotNull(message = "이벤트 상태값은 null일 수 없습니다.") EventStatus> eventStatusList;
    @FutureOrPresent(message = "시작일은 현재 또는 미래의 날짜여야 합니다.")
    private LocalDateTime startDate;
    @FutureOrPresent(message = "종료일은 현재 또는 미래의 날짜여야 합니다.")
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

    public RegionLocation getSingleRegionLocation() {
        if (regionLocationList == null || regionLocationList.size() != 1) {
            return null;
        }
        return regionLocationList.get(0);
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof EventListFilter that)) {
            return false;
        }
        return Objects.equals(costRange, that.costRange) && Objects.equals(
            locationList, that.locationList) && Objects.equals(regionLocationList,
            that.regionLocationList) && Objects.equals(eventCategoryList,
            that.eventCategoryList) && Objects.equals(eventStatusList, that.eventStatusList)
            && Objects.equals(startDate, that.startDate) && Objects.equals(endDate,
            that.endDate) && Objects.equals(categoryId, that.categoryId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(costRange, locationList, regionLocationList, eventCategoryList,
            eventStatusList, startDate, endDate, categoryId);
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

    @Override
    public String toString() {
        return "EventListFilter{" +
            "costRange=" + costRange +
            ", locationList=" + locationList +
            ", regionLocationList=" + regionLocationList +
            ", eventCategoryList=" + eventCategoryList +
            ", eventStatusList=" + eventStatusList +
            ", startDate=" + startDate +
            ", endDate=" + endDate +
            ", categoryId=" + categoryId +
            '}';
    }
}
