# Waiting Queue Troubleshooting

## Fixes Applied

- `WAITING_USER_IDS:{eventId}`에 `putIfAbsent`를 적용해 중복 진입을 1차 차단했다.
- `ENTRY_TOKEN`은 per-user key로 저장하고 5분 TTL을 직접 걸었다.
- `sseConnection == null`이면 즉시 정리 후 ACK하도록 바꿔 NPE와 pending 누적을 막았다.
- Lua 스크립트는 중간 실패 시 스킵하고, 실제 승격 수만 반환하도록 바꿨다.
- `ENTRY_QUEUE_SLOTS`는 seat count가 아니라 slot count로 해석하도록 고정했다.

## Quantitative Checks

- token TTL: `300s`
- scheduler cadence: `1s`
- duplicate guard scope: `1 userId / 1 eventId`
- slot return: `+1` per disconnected in-progress user

## Evidence

- `docs/troubleshooting/entry-dispatch-sse-null-ack.md`
- `docs/troubleshooting/waiting-queue-in-user-record.md`
- `docs/troubleshooting/entry-token-ttl.md`
- `docs/troubleshooting/entry-queue-count-meaning.md`
- `docs/troubleshooting/promotion-partial-failure.md`

## Related Docs

- [Use Case](../usecase/waiting-queue.md)
- [Flow](../flow/waiting-queue.md)
- [Trouble](../trouble/waiting-queue.md)
