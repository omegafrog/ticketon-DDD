# GET /api/v1/monitoring/threadpool

- Controller: `MonitoringController.getThreadPoolStatus()`
- Actor: 외부 호출자
- Goal: 대기열 진입, 연결 해제, polling 상태 조회, 운영 모니터링을 처리한다.
- Source: `/mnt/e/workspace/ticketon-DDD/broker/src/main/java/org/codenbug/broker/ui/MonitoringController.java`

## Use Case

대기열 진입, 연결 해제, polling 상태 조회, 운영 모니터링을 처리한다.

## Success Criteria

- 요청은 `GET` `/api/v1/monitoring/threadpool` 기준으로 해석한다.
- 컨트롤러는 자신이 맡은 입력 검증과 서비스 호출까지만 책임진다.
- 응답은 `ResponseEntity<RsData<Map<String, Object>>>` 기준으로 외부 계약을 유지한다.

## Related Docs

- [Flow](../../flow/broker/MonitoringController/getThreadPoolStatus.md)
- [Trouble](../../trouble/broker/MonitoringController/getThreadPoolStatus.md)
- [Troubleshooting](../../troubleshooting/broker/MonitoringController/getThreadPoolStatus.md)
