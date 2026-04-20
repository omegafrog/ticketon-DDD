# Waiting Queue

## Actor

- 사용자
- 운영자
- event service

## Goal

사용자는 대기열에 안전하게 진입하고, 본인 순번과 승격 상태를 실시간으로 받고 싶다.
운영자는 scale-out 환경에서도 중복 진입과 순번 꼬임이 없기를 원한다.

## Use Cases

### 1. 대기열 진입

- 액터: 사용자
- 시작점: broker의 SSE 엔드포인트
- 기대 결과: userId와 eventId가 1회만 등록되고, 이후 순번 정보가 흘러간다.

### 2. 대기열 연결 해제

- 액터: 사용자
- 기대 결과: 연결이 끊기면 남은 slot과 토큰 정리가 수행된다.

### 3. 승격 모니터링

- 액터: 운영자
- 기대 결과: dispatcher와 broker의 큐 상태를 통해 승격 지연을 추적할 수 있다.

## Success Criteria

- 동일 userId는 같은 eventId에서 중복 진입하지 않는다.
- 승격은 순번 기준으로 유지된다.
- 연결 해제 시 slot 반환과 토큰 정리가 일관되게 이뤄진다.

## Related Docs

- [Flow](../flow/waiting-queue.md)
- [Trouble](../trouble/waiting-queue.md)
- [Troubleshooting](../troubleshooting/waiting-queue.md)
- Legacy: `docs/broker/README.md`
- Legacy: `docs/dispatcher/waitingqueue.md`
