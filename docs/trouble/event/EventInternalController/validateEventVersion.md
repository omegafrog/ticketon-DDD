# GET /internal/events/{eventId}/version-check Trouble

## Before

- purchase 쪽에서 이벤트 상세를 직접 읽어 버전과 상태를 대조하면 검증 로직이 쉽게 복제된다.
- 내부 검증은 외부 사용자 API보다 가볍고 결정적이어야 한다.
- 문자열 status parsing 실패까지 business failure로 흘러가면 confirm worker가 잘못 retry할 수 있다.

## Decision Points

- version/status 검증을 boolean API로 분리해서 caller가 guard로만 사용하게 한다.
- 파싱 실패는 `400`으로 끊고, 정상 파싱 후 불일치만 `false`로 돌려준다.
- event repository가 authoritative source가 되도록 purchase가 직접 상세 필드를 조합하지 않는다.

## Failure Modes

- 검증이 너무 무거우면 purchase confirm latency가 늘고, retry window가 넓어진다.
- status의 의미가 purchase와 event에서 다르게 해석되면 stale payment가 통과할 수 있다.
- parse error를 false로 뭉개면 caller는 invalid request와 genuine mismatch를 구분하지 못한다.

## Why It Matters

- 이 경계는 결제의 안전장치이자 이벤트 변경의 단일 진실 공급원이다.
- 한 번의 버전 누락이 잘못된 결제 확정으로 직결될 수 있다.

## Recent History

- [event-repository] `522f561` (2026-04-01): feat(purchase): EventSourcing 기반 결제 Confirm 플로우 + 스케줄러 기반 재시도 (#9)
- [controller] `9c6c823` (2026-03-31): refactor: unify RsData responses and split common/domain exceptions (#7)
- [controller] `ed148bf` (2026-02-05): feat: 결제 모듈 이벤트 소싱 전환 및 이벤트 점유(Hold) 로직 구현
- [event-query] `105d681` (2026-01-22): feat: 문서 정리 및 API 문서 삭제
- [controller] `c48631b` (2026-01-21): feat: 이벤트 상태 및 버전 검증 로직 추가
- [event-repository] `0f82549` (2026-01-21): feat: 좌석 상태 업데이트 및 이벤트 삭제 로직 개선
- [controller] `3fa11c3` (2026-01-20): delete: 불필요한 파일 및 설정 제거
- [event-query] `dd1dae9` (2025-08-20): feat: 매니저 이벤트 리스트 조회 API 추가
- [event-repository] `fbe21df` (2025-08-20): feat: 이벤트 검색 로직 개선
- [event-query] `b856823` (2025-07-22): feat: 문서화 추가
- [event-query] `10662e7` (2025-07-20): feat: 프런트 연동



## Related Docs

- [Use Case](../../usecase/event/EventInternalController/validateEventVersion.md)
- [Flow](../../flow/event/EventInternalController/validateEventVersion.md)
- [Troubleshooting](../../troubleshooting/event/EventInternalController/validateEventVersion.md)
