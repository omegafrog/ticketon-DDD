# Event/Broker/Dispatcher API Load Assessment

본 문서는 event, broker, dispatcher 모듈 기준으로 고부하 시 발생 가능한 사이드 이펙트/위험도를 요약합니다.

## Event
- [Med] `POST /api/v1/events/list` — 대형 조인 + Redis viewCount 조회로 DB/Redis 동시 부하.
- [Med] `GET /api/v1/events/{id}` — 단건 조회 + Redis 조회/증가(비동기); Redis 증가 폭주 가능.
- [Low] `GET /api/v1/events/manager/me` — 매니저 이벤트 리스트 조회; DB read 중심.
- [Med] `POST /api/v1/events` — 이벤트/좌석 레이아웃 생성 + 이벤트 발행; 트랜잭션 경합 위험.
- [Med] `PUT /api/v1/events/{eventId}` — 좌석 레이아웃 업데이트 + 캐시 무효화; 캐시 미스 증가.
- [Low] `DELETE /api/v1/events/{eventId}` — soft delete 성격 추정, 비교적 낮음.
- [Low] `PATCH /api/v1/events/{eventId}` — 상태 변경만 수행.

## Broker
- [High] `GET /api/v1/broker/events/{id}/tickets/waiting` — SSE 장기 연결 + Redis 큐 작업; 연결 누적 시 메모리/Redis 압박.
- [Med] `POST /api/v1/broker/events/{id}/tickets/disconnect` — Redis 정리 작업; 대량 호출 시 write burst.
- [Low] `GET /api/v1/monitoring/threadpool` — JMX 조회; 요청 폭주 시 오버헤드 증가.
- [Low] `GET /api/v1/monitoring/threadpool/summary` — 동일.

## Dispatcher
- 외부 노출 API 없음(컨트롤러 미탐지). 스케줄러/Redis Stream 소비 스레드 기반 비동기 처리만 존재.
