# ENTRY_QUEUE_SLOTS 의미 정리

## 문제
- ENTRY_QUEUE_SLOTS를 "남은 좌석 수"로 해석하면 IN_PROGRESS 종료 시 +1 로직이 좌석 초과를 유발할 수 있다.
- 반대로 "승격 가능한 슬롯 수"로 해석하면 IN_PROGRESS 종료 시 +1은 정상 동작이다.

## 결론
- ENTRY_QUEUE_SLOTS는 **대기열에서 entry로 이동 가능한 슬롯 수**로 사용한다.
- 따라서 IN_PROGRESS 상태의 연결이 종료되면 해당 슬롯을 반환해야 한다.

## 적용 내용
- IN_PROGRESS 종료 시 `ENTRY_QUEUE_SLOTS`를 +1 증가시키는 로직 유지
- 토큰 삭제 등 정리 로직은 기존 유지

## 관련 코드
- `broker/src/main/java/org/codenbug/broker/service/SseEmitterService.java`

