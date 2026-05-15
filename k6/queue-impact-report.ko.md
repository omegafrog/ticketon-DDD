# 대기열 적용 전후 부하 테스트 보고서

## 1. 목적

대기열을 사용하지 않는 직접 호출 방식과 대기열을 거친 호출 방식을 동일한 대상 API 기준으로 비교했다.

비교 대상은 `GET /api/v1/events/{eventId}/seats`이다. 이 API는 실제 좌석 도메인을 조회하므로, 대기열이 하위 서비스 부하를 얼마나 완화하는지 확인하기에 적합하다.

## 2. 테스트 작성 흐름

1. 비교 모드 분리

   `k6/queue-impact-comparison.js`에 `MODE=direct`와 `MODE=queue`를 둔다.

   `direct` 모드는 로그인 후 바로 대상 API를 호출한다.

   `queue` 모드는 로그인 후 브로커 polling 대기열에 진입하고, `ENTRY` 토큰을 받을 때까지 polling한 뒤 동일한 대상 API를 호출한다.

2. 동일한 대상 API 사용

   두 모드 모두 최종 대상은 같은 API로 고정했다.

   ```text
   GET /api/v1/events/{eventId}/seats
   ```

   이렇게 해야 차이가 API 종류 때문이 아니라 대기열 유무 때문에 발생했는지 볼 수 있다.

3. 테스트 데이터 시드 작성

   `k6/seed-queue-comparison-data.sh`에서 부하 테스트용 행사, 좌석 레이아웃, 좌석, 로그인 사용자를 생성한다.

   주요 데이터는 다음과 같다.

   ```text
   eventId: event-k6-001
   seatLayoutId: 9001
   available seats: 6
   users: user{n}@example.com / password123!
   ```

4. 결과 비교 스크립트 작성

   `k6/compare_queue_results.py`는 direct summary JSON과 queue summary JSON을 읽어 주요 지표를 나란히 출력한다.

   비교 지표는 완료 사용자 수, HTTP p95, 대상 API p95, end-to-end 시간, 실패율, 대기 시간이다.

## 3. 실행 전 확인 사항

테스트 전 다음 서비스가 필요하다.

```bash
docker-compose -f docker/docker-compose.yml up -d
./gradlew :platform:eureka:bootRun
./gradlew :platform:gateway:bootRun
./gradlew :app:bootRun
./gradlew :auth:bootRun
./gradlew :broker:bootRun
./gradlew :dispatcher:bootRun
```

polling 대기열 테스트에서는 broker와 dispatcher가 같은 Redis를 봐야 한다.

현재 로컬 polling Redis는 `6381`이고, dispatcher 기본값도 `6381`로 맞췄다.

## 4. 실행 명령

테스트 데이터 생성:

```bash
./k6/seed-queue-comparison-data.sh
```

Direct 모드:

```bash
k6 run \
  -e MODE=direct \
  -e BASE_URL=http://localhost:8080 \
  -e EVENT_IDS=event-k6-001 \
  -e VUS=20 \
  -e TEST_DURATION=30s \
  -e USER_EMAIL_PREFIX=user \
  -e LOGIN_EMAIL_DOMAIN=example.com \
  -e DEFAULT_PASSWORD='password123!' \
  -e SUMMARY_FILE=queue-impact-direct-summary.json \
  k6/queue-impact-comparison.js
```

Queue 모드:

```bash
k6 run \
  -e MODE=queue \
  -e BASE_URL=http://localhost:8080 \
  -e BROKER_BASE_URL=http://localhost:8080 \
  -e EVENT_IDS=event-k6-001 \
  -e VUS=20 \
  -e TEST_DURATION=30s \
  -e USER_EMAIL_PREFIX=user \
  -e LOGIN_EMAIL_DOMAIN=example.com \
  -e DEFAULT_PASSWORD='password123!' \
  -e SUMMARY_FILE=queue-impact-queue-summary.json \
  k6/queue-impact-comparison.js
```

결과 비교:

```bash
python3 k6/compare_queue_results.py \
  queue-impact-direct-summary.json \
  queue-impact-queue-summary.json
```

## 5. 실행 결과

테스트 조건:

```text
VUs: 20
eventId: event-k6-001
gateway: http://localhost:8080
target: GET /api/v1/events/event-k6-001/seats
```

| 지표 | Direct | Queue | 해석 |
| --- | ---: | ---: | --- |
| 완료 사용자 | 20 | 20 | 두 모드 모두 전체 완료 |
| 성공률 | 100% | 100% | 기능 실패 없음 |
| 대상 API 요청 수 | 20 | 20 | 같은 대상 API를 같은 횟수 호출 |
| 대상 API p95 | 97.10 ms | 47.45 ms | 대기열 적용 시 실제 좌석 API 부하가 완화됨 |
| end-to-end p95 | 97.10 ms | 20,318.30 ms | 사용자는 대기열 시간만큼 늦게 API에 도달 |
| 대기열 대기 p95 | n/a | 20,143.15 ms | queue 모드의 핵심 비용 |
| polling current p95 | n/a | 47.85 ms | polling 응답 자체는 낮은 지연 |
| HTTP 실패율 | 0.00 | 0.00 | 네트워크/HTTP 실패 없음 |

## 6. 해석

Direct 모드는 모든 사용자가 즉시 좌석 API에 도달한다. 따라서 사용자 관점의 end-to-end 시간은 짧지만, 순간적으로 하위 좌석 조회 API에 부하가 집중된다.

Queue 모드는 사용자가 먼저 브로커 대기열에 들어가고, dispatcher가 entry token을 발급한 뒤에만 좌석 API에 도달한다. 이 때문에 좌석 API 자체의 p95는 `97.10 ms`에서 `47.45 ms`로 낮아졌다. 반면 사용자가 최종 API를 완료하기까지의 p95는 `20,318.30 ms`로 증가했다.

현재 `event-k6-001`은 사용 가능한 좌석이 6개이므로 20명이 한 번에 모두 진입하지 않고 배치처럼 순차 진입한다. 그래서 queue 모드의 총 완료 시간과 queue wait time이 크게 나온다.

## 7. 테스트 중 발견한 문제와 조치

1. broker가 행사 응답의 `seatCount`만 읽고 있었다.

   현재 행사 단건 응답은 `seatCount:null`, `availableSeatCount:6` 형태였다. 이 때문에 broker가 entry slot을 `0`으로 초기화했고, dispatcher가 사용자를 승격하지 못했다.

   조치: `broker/src/main/java/org/codenbug/broker/infra/RestEventClient.java`에서 `seatCount`가 null이면 `availableSeatCount`를 fallback으로 사용하도록 변경했다.

2. broker와 dispatcher가 서로 다른 Redis를 보고 있었다.

   broker polling profile은 Redis `6381`을 사용했지만 dispatcher는 기본값 `6379`를 사용했다. broker가 `6381`에 entry stream을 써도 dispatcher가 읽지 못해 승격이 멈췄다.

   조치: `dispatcher/src/main/resources/application.yml`의 Redis 기본 포트를 `6381`로 맞췄다.

3. Redis `FLUSHDB` 후 consumer group이 사라졌다.

   대기열 Redis를 비울 때 `ENTRY_QUEUE` consumer group도 삭제되면 dispatcher가 `NOGROUP` 오류를 낸다.

   조치: benchmark 전 Redis를 정리할 때는 `ENTRY` stream과 consumer group을 보존하거나, 정리 후 dispatcher를 다시 시작해야 한다.

## 8. 결론

대기열은 하위 좌석 API의 순간 부하를 줄인다. 이번 20 VU 테스트에서는 대상 API p95가 약 `51%` 감소했다.

대신 사용자는 대기열에서 기다리므로 end-to-end p95는 약 `20.3초`로 증가했다.

즉, 대기열의 효과는 "전체 요청을 빠르게 만드는 것"이 아니라 "하위 시스템에 들어오는 요청을 제어해서 장애 가능성을 낮추는 것"이다.
