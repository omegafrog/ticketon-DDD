# Event 모듈 N+1 및 개선 내용

## 문제 상황
`EventQueryService.getManagerEvents()`에서 이벤트 목록을 가져온 뒤, 각 이벤트마다 좌석 레이아웃을 조회하면서 N+1이 발생했습니다.

```java
public Page<EventManagerListResponse> getManagerEvents(ManagerId managerId, Pageable pageable) {
    Page<Event> eventPage = eventRepository.getManagerEventList(managerId, pageable);
    List<EventCategory> categories = eventCategoryService.findAllByIds(
        eventPage.map(event -> event.getEventInformation().getCategoryId().getValue()).toList());

    return eventPage.map(event -> {
        EventCategory category = categories.stream()
            .filter(c -> c.getId().getId().equals(event.getEventInformation().getCategoryId().getValue()))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Category not found"));

        SeatLayout seatLayout = seatLayoutRepository.findSeatLayout(event.getSeatLayoutId().getValue());

        return new EventManagerListResponse(
            String.valueOf(event.getEventId().getEventId()),
            event.getEventInformation().getTitle(),
            event.getEventInformation().getCategoryId(),
            event.getEventInformation().getThumbnailUrl(),
            event.getEventInformation().getStatus(),
            event.getEventInformation().getEventStart(),
            event.getEventInformation().getEventEnd(),
            seatLayout.getLocation().getLocationName(),
            seatLayout.getLocation().getHallName(),
            event.getMetaData().getDeleted(),
            event.getEventInformation().getBookingStart(),
            event.getEventInformation().getBookingEnd()
        );
    });
}
```

## 원인
- 이벤트 목록 1회 조회
- 이후 이벤트 수만큼 `seat_layout` 조회가 반복 수행

## 해결 방법
좌석 레이아웃을 ID 목록으로 한 번에 조회한 뒤, 맵으로 캐싱하여 재사용하도록 변경했습니다.

```java
public Page<EventManagerListResponse> getManagerEvents(ManagerId managerId, Pageable pageable) {
    Page<Event> eventPage = eventRepository.getManagerEventList(managerId, pageable);
    List<EventCategory> categories = eventCategoryService.findAllByIds(
        eventPage.map(event -> event.getEventInformation().getCategoryId().getValue()).toList());

    List<Long> seatLayoutIds = eventPage.map(event -> event.getSeatLayoutId().getValue()).toList();
    Map<Long, SeatLayout> seatLayoutsById = seatLayoutRepository.findSeatLayouts(seatLayoutIds)
        .stream()
        .collect(Collectors.toMap(SeatLayout::getId, Function.identity()));

    return eventPage.map(event -> {
        EventCategory category = categories.stream()
            .filter(c -> c.getId().getId().equals(event.getEventInformation().getCategoryId().getValue()))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Category not found"));

        SeatLayout seatLayout = seatLayoutsById.get(event.getSeatLayoutId().getValue());
        if (seatLayout == null) {
            throw new IllegalArgumentException("Seat layout not found");
        }

        return new EventManagerListResponse(
            String.valueOf(event.getEventId().getEventId()),
            event.getEventInformation().getTitle(),
            event.getEventInformation().getCategoryId(),
            event.getEventInformation().getThumbnailUrl(),
            event.getEventInformation().getStatus(),
            event.getEventInformation().getEventStart(),
            event.getEventInformation().getEventEnd(),
            seatLayout.getLocation().getLocationName(),
            seatLayout.getLocation().getHallName(),
            event.getMetaData().getDeleted(),
            event.getEventInformation().getBookingStart(),
            event.getEventInformation().getBookingEnd()
        );
    });
}
```

### 관련 변경
```java
public interface SeatLayoutRepository {
    SeatLayout findSeatLayout(Long id);
    SeatLayout save(SeatLayout seatLayout);
    List<SeatLayout> findSeatLayouts(List<Long> ids);
}
```
