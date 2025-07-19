package org.codenbug.event.infra;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import org.codenbug.event.domain.Event;
import org.codenbug.event.domain.EventStatus;
import org.codenbug.event.global.EventListFilter;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

public class EventSpecification {

	/**
	 * 기본 조건: 삭제되지 않은 이벤트
	 */
	public static Specification<Event> isNotDeleted() {
		return (root, query, builder) -> builder.isFalse(root.get("isDeleted"));
	}

	/**
	 * 키워드(제목) 포함 조건
	 */
	public static Specification<Event> titleContains(String keyword) {
		if (!StringUtils.hasText(keyword)) {
			return null; // 조건이 없으면 무시
		}
		// 'information' Embeddable 객체 내부의 'title' 필드에 접근
		return (root, query, builder) -> builder.like(builder.lower(root.get("information").get("title")), "%" + keyword.toLowerCase() + "%");
	}

	/**
	 * 필터 객체를 기반으로 동적 Specification 생성 (개선된 버전)
	 * Stream.reduce를 사용하여 deprecated된 where() 없이 명세(Specification)를 조합합니다.
	 */
	public static Specification<Event> fromFilter(EventListFilter filter) {
		if (filter == null) {
			return null;
		}

		// 각 필터 조건을 Stream으로 만들고, null이 아닌 것들만 and 조건으로 조합합니다.
		return Stream.of(
				locationIn(filter.getLocationList()),
				categoryIn(filter.getEventCategoryList()),
				statusIn(filter.getEventStatusList()),
				dateBetween(filter.getStartDate().toLocalDate(), filter.getEndDate().toLocalDate()),
				costInRange(filter.getCostRange().getMin(), filter.getCostRange().getMax())
			)
			.filter(Objects::nonNull)
			.reduce(Specification::and)
			.orElse(null); // 모든 필터 조건이 비어있으면 null을 반환합니다.
	}

	// --- 개별 필터 조건 (기존과 동일) ---

	private static Specification<Event> locationIn(List<String> locations) {
		if (locations == null || locations.isEmpty()) return null;
		return (root, query, builder) -> root.get("information").get("location").in(locations);
	}

	private static Specification<Event> categoryIn(List<Long> categories) {
		if (categories == null || categories.isEmpty()) return null;
		return (root, query, builder) -> root.get("category").in(categories);
	}

	private static Specification<Event> statusIn(List<EventStatus> statuses) {
		if (statuses == null || statuses.isEmpty()) return null;
		return (root, query, builder) -> root.get("status").in(statuses
			.stream().map(status->status.name()).toList());
	}

	private static Specification<Event> dateBetween(LocalDate start, LocalDate end) {
		if (start == null || end == null) return null;
		return (root, query, builder) -> builder.between(root.get("bookingStart"), start, end);
	}

	private static Specification<Event> costInRange(Integer minCost, Integer maxCost) {
		if (minCost == null && maxCost == null) return null;

		return (root, query, builder) -> {
			if (minCost != null && maxCost != null) {
				return builder.and(
					builder.greaterThanOrEqualTo(root.get("minPrice"), minCost),
					builder.lessThanOrEqualTo(root.get("maxPrice"), maxCost)
				);
			}
			if (minCost != null) {
				return builder.greaterThanOrEqualTo(root.get("minPrice"), minCost);
			}
			return builder.lessThanOrEqualTo(root.get("maxPrice"), maxCost);
		};
	}
}