# Seat 모듈 N+1 상황 및 대응

## 문제 상황
좌석 레이아웃 목록을 조회한 뒤, 각 레이아웃의 좌석을 접근하면 N+1이 발생합니다.

```java
@OneToMany(mappedBy = "seatLayout", fetch = FetchType.LAZY, cascade = {CascadeType.PERSIST, CascadeType.MERGE,
    CascadeType.REMOVE}, orphanRemoval = true)
private Set<Seat> seats;
```

예: `SeatLayout` 목록을 가져온 뒤 `layout.getSeats()`를 순회하면 레이아웃 수만큼 추가 쿼리가 발생합니다.

## 원인
- `SeatLayout -> Seat` 연관이 LAZY
- 목록 조회 후 컬렉션 접근 시마다 별도 SELECT 수행

## 해결 방법
이번 변경에서는 `Event` 조회 최적화를 위해 ID 목록 기반 조회 메서드를 추가했습니다.

```java
public interface SeatLayoutRepository {
    SeatLayout findSeatLayout(Long id);
    SeatLayout save(SeatLayout seatLayout);
    List<SeatLayout> findSeatLayouts(List<Long> ids);
}
```

N+1 자체를 제거하려면 아래 중 하나를 적용해야 합니다.
- `JpaSeatRepository`에 fetch join 또는 `@EntityGraph` 쿼리 추가
- 서비스 계층에서 ID 목록으로 한 번에 조회 후 매핑

현재는 N+1 재현 테스트를 유지하여, 문제가 발생하는 패턴을 계속 감지하도록 했습니다.
