# Event Storming — Ticketon DDD (Use Case 중심)

> 코드베이스(Controller → Service → Domain → Event/Consumer) 전부를 분석하여,
> **하나의 유스케이스 = Command → Event → Policy** 순서로 정리했다.

---

## 범례

| 마커 | 의미 | 설명 |
|------|------|------|
| 🟠 **Command** | 유스케이스 진입점 | Actor가 시스템에 요청하는 행위 (HTTP endpoint) |
| 🔵 **Event** | 도메인 이벤트 | 시스템 내에서 발생한 사실 (Fact). 이미 일어난 일. |
| 🟢 **Policy** | 반응 규칙 | "When Event → Then Action". 이벤트를 트리거로 하는 자동 응답. |
| ⬛ **Read Model** | 조회 결과 | Query로 반환되는 읽기 전용 데이터. 상태 변경 없음. |

---

## Actor

| Actor | 설명 |
|-------|------|
| **User** | 일반 사용자 (티켓 예매) |
| **Manager** | 이벤트 관리자 (이벤트 생성·수정·환불) |
| **Admin** | 시스템 관리자 (알림 관리·전체 환불) |
| **System** | 자동 트리거 (Dispatcher 스케줄러, RabbitMQ Consumer, Redis Stream Listener) |
| **PG(Toss)** | 외부 결제 게이트웨이 |

---

## UC-1. 이메일 회원가입

**🟠 Command:** `POST /api/v1/auth/register` — `RegisterUser`  
**Actor:** User

```
[User] ── RegisterUser(RegisterRequest)
  │
  ▼
  Auth Service: AuthService.register()
    └─ SecurityUser 생성 (이메일/비밀번호 해싱)
  │
  ▼
🔵 SecurityUserRegisteredEvent (RabbitMQ)
   payload: securityUserId, name, age, sex, phoneNum, location
  │
  ▼
🟢 Policy: User 프로필 자동 생성
   Consumer: SecurityUserRegisteredConsumer
   ├─ User 도메인에서 User 프로필 생성
   │
   ▼
   🔵 UserRegisteredEvent (RabbitMQ)  [성공 시]
   🔵 UserRegisteredFailedEvent (RabbitMQ)  [실패 시]
  │
  ▼
🟢 Policy: Auth에 userId 링크
   Consumer: UserRegisteredEventConsumer
   └─ SecurityUser에 business userId 연결
```

| 구분 | 이름 | Transport | Consumer | Action |
|------|------|-----------|----------|--------|
| 🔵 Event | `SecurityUserRegisteredEvent` | RabbitMQ | User | User 프로필 생성 트리거 |
| 🔵 Event | `UserRegisteredEvent` | RabbitMQ | Auth | SecurityUser에 userId 링크 |
| 🔵 Event | `UserRegisteredFailedEvent` | RabbitMQ | Auth | SecurityUser 정리 (보상) |
| 🟢 Policy | User 프로필 자동 생성 | — | SecurityUserRegisteredConsumer | User 생성 → 성공/실패 이벤트 발행 |
| 🟢 Policy | Auth에 userId 링크 | — | UserRegisteredEventConsumer | SecurityUser 업데이트 |

---

## UC-2. 소셜 로그인 (Google/Kakao)

**🟠 Command:** `GET /api/v1/auth/social/{type}/callback` — `SocialLoginCallback`  
**Actor:** User

```
[User] ── SocialLoginCallback(code)
  │
  ▼
  Auth Service: OAuthService.requestAccessTokenAndSaveUser()
    ├─ 소셜 Provider에 access token 요청
    ├─ 사용자 정보 수신
    └─ 신규 사용자면 SecurityUser 생성
  │
  ▼
🔵 SnsUserRegisteredEvent (RabbitMQ)
   payload: securityUserId, name, age, sex
  │
  ▼
🟢 Policy: SNS User 프로필 자동 생성
   Consumer: SnsUserRegisteredConsumer
   ├─ User 도메인에서 SNS 사용자 프로필 생성
   │
   ▼
   🔵 UserRegisteredEvent (RabbitMQ)  [성공 시]
   🔵 UserRegisteredFailedEvent (RabbitMQ)  [실패 시]
  │
  ▼
🟢 Policy: Auth에 userId 링크
   Consumer: UserRegisteredEventConsumer
   └─ SecurityUser에 business userId 연결
  │
  ▼
  [User] ← AccessToken(Header) + RefreshToken(Cookie)
```

| 구분 | 이름 | Transport | Consumer | Action |
|------|------|-----------|----------|--------|
| 🔵 Event | `SnsUserRegisteredEvent` | RabbitMQ | User | SNS User 프로필 생성 트리거 |
| 🔵 Event | `UserRegisteredEvent` | RabbitMQ | Auth | SecurityUser에 userId 링크 |
| 🔵 Event | `UserRegisteredFailedEvent` | RabbitMQ | Auth | SecurityUser 정리 (보상) |
| 🟢 Policy | SNS User 프로필 자동 생성 | — | SnsUserRegisteredConsumer | User 생성 → 성공/실패 이벤트 발행 |
| 🟢 Policy | Auth에 userId 링크 | — | UserRegisteredEventConsumer | SecurityUser 업데이트 |

---

## UC-3. 이메일 로그인

**🟠 Command:** `POST /api/v1/auth/login` — `LoginWithEmail`  
**Actor:** User

```
[User] ── LoginWithEmail(email, password)
  │
  ▼
  Auth Service: AuthService.loginEmail()
    ├─ 이메일/비밀번호 검증
    ├─ SecurityUser 조회
    ├─ JWT AccessToken 생성
    └─ JWT RefreshToken 생성
  │
  ▼
  [User] ← AccessToken(Header) + RefreshToken(Cookie)
```

> 이 유스케이스는 도메인 이벤트 없이 동기적으로 완료됨.

---

## UC-4. 로그아웃

**🟠 Command:** `GET /api/v1/auth/logout` — `Logout`  
**Actor:** User

```
[User] ── Logout
  │
  ▼
  Auth Service:
    ├─ RefreshToken 쿠키 삭제 (MaxAge=0)
    └─ RefreshToken 블랙리스트 등록 (Redis)
  │
  ▼
  [User] ← 로그아웃 완료
```

> 이 유스케이스는 도메인 이벤트 없이 동기적으로 완료됨.

---

## UC-5. 토큰 재발급

**🟠 Command:** `POST /api/v1/auth/refresh` — `RefreshTokens`  
**Actor:** User

```
[User] ── RefreshTokens(RefreshToken in Cookie)
  │
  ▼
  Auth Service: AuthService.refreshTokens()
    ├─ RefreshToken 유효성 검증
    ├─ 블랙리스트 확인
    └─ 새 AccessToken + RefreshToken 발급
  │
  ▼
  [User] ← 새 AccessToken(Header) + 새 RefreshToken(Cookie)
```

> 이 유스케이스는 도메인 이벤트 없이 동기적으로 완료됨.

---

## UC-6. 내 프로필 조회

**🟠 Command:** `GET /api/v1/users/me` — `GetMyProfile`  
**Actor:** User / Manager

```
[User] ── GetMyProfile
  │
  ▼
  User Service: UserQueryService.findMe()
    ├─ LoggedInUserContext에서 userId 추출
    └─ User 프로필 조회
  │
  ▼
⬛ Read Model: UserInfo (name, age, sex, phoneNum, location)
```

> 조회 전용 — 도메인 이벤트 없음.

---

## UC-7. 내 프로필 수정

**🟠 Command:** `PUT /api/v1/users/me` — `UpdateMyProfile`  
**Actor:** User

```
[User] ── UpdateMyProfile(UpdateUserRequest)
  │
  ▼
  User Service: UserProfileCommandService.updateUser()
    ├─ LoggedInUserContext에서 userId 추출
    └─ User 프로필 업데이트 (name, age, sex, phoneNum, location)
  │
  ▼
  [User] ← 업데이트된 UserInfo
```

> 이 유스케이스는 도메인 이벤트 없이 동기적으로 완료됨.

---

## UC-8. 이벤트 등록

**🟠 Command:** `POST /api/v1/events` — `RegisterEvent`  
**Actor:** Manager

```
[Manager] ── RegisterEvent(NewEventRequest)
  │
  ▼
  Event Service: RegisterEventService.registerNewEvent()
    ├─ 카테고리 존재 여부 검증
    ├─ 날짜/가격 검증 (bookingStart < bookingEnd < eventStart < eventEnd)
    ├─ Event Aggregate 생성 (UUIDv7 ID)
    └─ Seat Service에 좌석 레이아웃 생성 요청
  │
  ▼
🔵 EventCreatedEvent
   payload: eventId, title, managerId, seatLayoutId, seatSelectable,
            locationName, eventStart, eventEnd
  │
  ▼
🟢 Policy: Seat 레이아웃 연동
   ├─ Seat 도메인에서 SeatLayout 생성
   │
   ▼
   🔵 SeatLayoutCreatedEvent
  │
  ▼
  [Manager] ← EventId (201 Created)
```

| 구분 | 이름 | Transport | Consumer | Action |
|------|------|-----------|----------|--------|
| 🔵 Event | `EventCreatedEvent` | RabbitMQ | Seat | SeatLayout 생성 트리거 |
| 🔵 Event | `SeatLayoutCreatedEvent` | — | (내부) | 좌석 레이아웃 생성 완료 알림 |
| 🟢 Policy | Seat 레이아웃 연동 | — | Event→Seat | Seat 도메인에 레이아웃 생성 요청 |

---

## UC-9. 이벤트 수정

**🟠 Command:** `PUT /api/v1/events/{eventId}` — `UpdateEvent`  
**Actor:** Manager / Admin

```
[Manager] ── UpdateEvent(eventId, UpdateEventRequest)
  │
  ▼
  Event Service: UpdateEventService.updateEvent()
    ├─ 이벤트 소유자 검증 (Manager 본인 or Admin)
    ├─ 예약 기간 종료 후 수정 불가 검증
    └─ Event Aggregate 업데이트
     └─ Event.version++ (낙관적 락)
     └─ Event.version++ (낙관적 락)
     └─ Event.version++ (낙관적 락)
  │
  ▼
🔵 EventUpdatedEvent
   payload: eventId, 변경된 필드, version
  │
        ┌────────────────────┴────────────────────┐
        ▼                                         ▼
🟢 Policy: Seat 레이아웃 갱신              🟢 Policy: 이벤트 변경 알림
   ├─ SeatLayoutUpdatedEvent 발행              ├─ Notification에 알림 생성
   └─ Seat 도메인에서 레이아웃 반영            └─ 사용자에게 변경 사항 통지
  │
  ▼
🔵 EventNonCoreUpdatedEvent  (부수 정보만 변경 시)
```

| 구분 | 이름 | Transport | Consumer | Action |
|------|------|-----------|----------|--------|
| 🔵 Event | `EventUpdatedEvent` | RabbitMQ | Seat, Notification | 레이아웃 갱신 + 알림 트리거 |
| 🔵 Event | `SeatLayoutUpdatedEvent` | RabbitMQ | Seat | 좌석 레이아웃 갱신 |
| 🔵 Event | `EventNonCoreUpdatedEvent` | RabbitMQ | — | 부수 정보 변경 알림 |
| 🟢 Policy | Seat 레이아웃 갱신 | — | Seat | 좌석 레이아웃 업데이트 |
| 🟢 Policy | 이벤트 변경 알림 | — | Notification | 사용자에게 알림 전송 |
| 🟢 Policy | Event version++ (낙관적 락) | — | Event | 동시 수정 충돌 방지 |

---

## UC-10. 이벤트 삭제

**🟠 Command:** `DELETE /api/v1/events/{eventId}` — `DeleteEvent`  
**Actor:** Manager / Admin

```
[Manager] ── DeleteEvent(eventId)
  │
  ▼
  Event Service: UpdateEventService.deleteEvent()
    ├─ 이벤트 소유자 검증
    └─ Soft Delete (isDeleted = true)
  │
  ▼
🔵 EventDeletedEvent
   payload: eventId
  │
  ▼
🟢 Policy: 관련 좌석 레이아웃 무효화
   └─ Seat 도메인에서 해당 이벤트 좌석 비활성화
```

| 구분 | 이름 | Transport | Consumer | Action |
|------|------|-----------|----------|--------|
| 🔵 Event | `EventDeletedEvent` | RabbitMQ | Seat | 좌석 레이아웃 무효화 트리거 |
| 🟢 Policy | 관련 좌석 레이아웃 무효화 | — | Seat | 좌석 비활성화 |

---

## UC-11. 이벤트 상태 변경

**🟠 Command:** `PATCH /api/v1/events/{eventId}?status=` — `ChangeEventStatus`  
**Actor:** Manager / Admin

```
[Manager] ── ChangeEventStatus(eventId, status)
  │
  ▼
  Event Service: UpdateEventService.changeStatus()
    ├─ 이벤트 소유자 검증
    ├─ 상태 전이 검증 (CLOSED → OPEN 등)
    └─ EventInformation.status 업데이트
  │
  ▼
🔵 EventUpdatedEvent
   payload: eventId, status
```

| 구분 | 이름 | Transport | Consumer | Action |
|------|------|-----------|----------|--------|
| 🔵 Event | `EventUpdatedEvent` | RabbitMQ | Seat, Notification | 상태 변경 전파 |
| 🟢 Policy | Seat 레이아웃 갱신 | — | Seat | 상태에 따른 좌석 제어 |
| 🟢 Policy | 이벤트 변경 알림 | — | Notification | 사용자에게 상태 변경 알림 |

---

## UC-12. 이벤트 목록 조회

**🟠 Command:** `POST /api/v1/events/list` — `SearchEvents`  
**Actor:** Public

```
[Public] ── SearchEvents(keyword?, EventListFilter?, Pageable)
  │
  ▼
  Event Service: EventViewRepository.findEventList()
    ├─ Elasticsearch 키워드 검색
    ├─ 필터 적용 (category, location, price, status, date)
    ├─ Projection 조회 (N+1 방지)
    └─ Redis에서 viewCount 병합
  │
  ▼
⬛ Read Model: Page<EventListProjection>
```

> 조회 전용 — 도메인 이벤트 없음.

---

## UC-13. 이벤트 상세 조회

**🟠 Command:** `GET /api/v1/events/{id}` — `GetEventDetail`  
**Actor:** Public

```
[Public] ── GetEventDetail(id)
  │
  ▼
  Event Service: EventViewRepository.findEventById()
    └─ Projection으로 단건 조회
  │
  ▼
🟢 Policy: 조회수 비동기 증가
   └─ EventViewCountService.incrementViewCountAsync()
      (응답에 영향 없음, 별도 스레드 처리)
  │
  ▼
⬛ Read Model: EventListProjection (상세 정보)
```

| 구분 | 이름 | Transport | Consumer | Action |
|------|------|-----------|----------|--------|
| 🟢 Policy | 조회수 비동기 증가 | — | EventViewCountService | Redis viewCount 증가 |

---

## UC-14. 매니저 내 이벤트 목록 조회

**🟠 Command:** `GET /api/v1/events/manager/me` — `GetMyManagerEvents`  
**Actor:** Manager

```
[Manager] ── GetMyManagerEvents
  │
  ▼
  Event Service: EventViewRepository.findManagerEventList(userId)
    ├─ LoggedInUserContext에서 매니저 userId 추출
    └─ 해당 매니저가 생성한 이벤트 목록 Projection 조회
  │
  ▼
⬛ Read Model: Page<EventListProjection>
```

> 조회 전용 — 도메인 이벤트 없음.

---

## UC-15. 대기열 진입 (SSE)

**🟠 Command:** `GET /api/v1/broker/events/{id}/tickets/waiting` — `EnterWaitingQueue`  
**Actor:** User

```
[User] ── EnterWaitingQueue(eventId)
  │
  ▼
  Broker Service: WaitingQueueEntryService.entry()
    ├─ 중복 진입 확인 (Redis: WAITING_USER_IDS:{eventId})
    ├─ Event Service에서 좌석 수 조회
    ├─ Redis Sorted Set에 추가 (ZADD WAITING:{eventId}, score=timestamp)
    ├─ Redis Hash에 메타데이터 저장 (HSET WAITING_QUEUE_INDEX_RECORD:{eventId})
    └─ SSE 연결 생성 (SseEmitter)
  │
  ▼
🔵 SSE 스트림 시작 (반복 이벤트)
   ├─ HEARTBEAT (5초마다) — payload: timestamp
   └─ QUEUE_POSITION (1초마다) — payload: position, totalWaiting
  │
  ▼ (Dispatcher가 승격 처리 후)
🔵 UserPromoted (DISPATCH Stream 수신)
   payload: userId, eventId, promotedAt
  │
  ▼
🟢 Policy: EntryToken 발급 + SSE 알림
   ├─ JWT EntryToken 생성 (5분 유효)
   ├─ SSE로 "구매 가능!" 알림 전송
   │  data: {"type":"PROMOTED", "entryToken":"jwt...", "message":"You can now purchase!"}
   └─ SseConnection.status = IN_PROGRESS
```

| 구분 | 이름 | Transport | Consumer | Action |
|------|------|-----------|----------|--------|
| 🔵 Event | `HEARTBEAT` | SSE | User (브라우저) | 연결 유지 |
| 🔵 Event | `QUEUE_POSITION` | SSE | User (브라우저) | 대기 순위 업데이트 |
| 🔵 Event | `UserPromoted` | Redis Stream (DISPATCH) | Broker | 승격 알림 수신 |
| 🟢 Policy | EntryToken 발급 + SSE 알림 | — | Broker | JWT 발급 → SSE 푸시 |

---

## UC-16. 대기열 명시적 이탈

**🟠 Command:** `POST /api/v1/broker/events/{id}/tickets/disconnect` — `DisconnectFromQueue`  
**Actor:** User

```
[User] ── DisconnectFromQueue(eventId)
  │
  ▼
  Broker Service: WaitingQueueEntryService.disconnect()
    ├─ Redis에서 대기열 정보 제거
    │  ├─ ZREM WAITING:{eventId} userId
    │  ├─ HDEL WAITING_QUEUE_INDEX_RECORD:{eventId} userId
    │  └─ HDEL WAITING_USER_IDS:{eventId} userId
    └─ SSE 연결 정리
  │
  ▼
  [User] ← 대기열 이탈 완료
```

> 이 유스케이스는 도메인 이벤트 없이 동기적으로 완료됨.

---

## UC-17. 대기열 승격 (Dispatcher 자동)

**🟠 Command:** `PromoteWaitingUsers` (스케줄러 1초마다)  
**Actor:** System

```
[System] ── Dispatcher.EntryPromoteThread.promote() (1초마다)
  │
  ▼
  ├─ SCAN으로 WAITING:* 키 발견
  └─ 임시 작업 목록 생성 (LPUSH temp:{uuid})
  │
  ▼ (10개 스레드 병렬 처리)
🟢 Policy: 원자적 승격 (Lua Script)
   ├─ ENTRY_QUEUE_SLOTS 확인 (잔여 capacity)
   ├─ ZRANGE WAITING:{eventId} 0 (capacity-1)
   ├─ 각 사용자:
   │   ├─ WAITING에서 제거 (ZREM, HDEL)
   │   └─ ENTRY Stream에 추가 (XADD)
   └─ ENTRY_QUEUE_SLOTS 증가 (INCRBY)
  │
  ▼
🔵 ENTRY Stream 메시지
   payload: userId, eventId, promotedAt
  │
  ▼
🟢 Policy: DISPATCH Stream forwarding
   Consumer: EntryQueueConsumer
   ├─ ENTRY Stream 메시지 소비
   ├─ DISPATCH Stream에 forwarding (XADD)
   └─ XACK로 메시지 확인
  │
  ▼
  [Broker가 DISPATCH Stream 소비 → SSE로 사용자 알림 (UC-15)]
```

| 구분 | 이름 | Transport | Consumer | Action |
|------|------|-----------|----------|--------|
| 🔵 Event | `ENTRY Stream 메시지` | Redis Stream | Dispatcher | 승격된 사용자 기록 |
| 🟢 Policy | 원자적 승격 (Lua Script) | — | Dispatcher | WAITING → ENTRY 원자적 이동 |
| 🟢 Policy | DISPATCH Stream forwarding | — | EntryQueueConsumer | ENTRY → DISPATCH 중계 |

---

## UC-18. 좌석 조회

**🟠 Command:** `GET /api/v1/events/{event-id}/seats` — `GetSeatLayout`  
**Actor:** User

```
[User] ── GetSeatLayout(eventId)
  │
  ▼
  Seat Service: FindSeatLayoutService.findSeatLayoutByEventId()
    ├─ EventId로 SeatLayout 조회
    ├─ 좌석 목록 (signature, grade, price, available)
    └─ 레이아웃 그리드 정보
  │
  ▼
⬛ Read Model: SeatLayoutResponse
```

> 조회 전용 — 도메인 이벤트 없음.

---

## UC-19. 좌석 선택

**🟠 Command:** `POST /api/v1/events/{event-id}/seats` — `SelectSeat`  
**Actor:** User

```
[User] ── SelectSeat(eventId, SeatSelectRequest, entryAuthToken)
  │
  ▼
  ├─ EntryToken 검증 (userId, eventId 일치 확인)
  │
  ▼
  Seat Service: UpdateSeatLayoutService.selectSeat()
    ├─ 좌석 가용성 확인
    └─ Redis 분산 락 시도 (5분 TTL)
       lockKey = "seat:lock:{userId}:{eventId}:{seatId}"
  │
  ▼
🔵 Seat Reserved (도메인 이벤트)
   └─ seat.reserve() → available = false
  │
  ▼
⬛ Read Model: SeatSelectResponse (선택된 좌석, 총 금액)
```

| 구분 | 이름 | Transport | Consumer | Action |
|------|------|-----------|----------|--------|
| 🔵 Event | `Seat Reserved` | — (내부) | — | 좌석 예약 상태 변경 |
| 🟢 Policy | Redis 분산 락 획득 | — | Seat | 5분 TTL로 동시성 제어 |

---

## UC-20. 좌석 선택 취소

**🟠 Command:** `DELETE /api/v1/events/{event-id}/seats` — `CancelSeat`  
**Actor:** User

```
[User] ── CancelSeat(eventId, SeatCancelRequest, entryAuthToken)
  │
  ▼
  ├─ EntryToken 검증
  └─ 좌석 소유자 확인
  │
  ▼
  Seat Service: UpdateSeatLayoutService.cancelSeat()
    ├─ Redis 락 해제
    └─ seat.cancelReserve() → available = true
  │
  ▼
🔵 Seat Released (도메인 이벤트)
  │
  ▼
  [User] ← 좌석 취소 완료
```

| 구분 | 이름 | Transport | Consumer | Action |
|------|------|-----------|----------|--------|
| 🔵 Event | `Seat Released` | — (내부) | — | 좌석 가용 상태 복원 |
| 🟢 Policy | Redis 락 해제 | — | Seat | 동시성 락 반납 |

---

## UC-21. 결제 준비 (Initiate)

**🟠 Command:** `POST /api/v1/payments/init` — `InitiatePayment`  
**Actor:** User

```
[User] ── InitiatePayment(InitiatePaymentRequest, entryAuthToken)
  │
  ▼
  ├─ EntryToken 검증
  │
  ▼
  Purchase Service: PurchaseInitCommandService.initiatePayment()
    ├─ Event 존재 여부 검증 (EventServiceClient)
    ├─ 금액 검증
    └─ Purchase 엔티티 생성 (paymentStatus = IN_PROGRESS)
       └─ orderId, orderName, amount 설정
  │
  ▼
🔵 PaymentInitiated (도메인 이벤트)
   payload: purchaseId, eventId, userId, amount
  │
  ▼
⬛ Read Model: InitiatePaymentResponse (purchaseId, paymentStatus)
   → Toss Payments 결제 페이지로 리다이렉트
```

| 구분 | 이름 | Transport | Consumer | Action |
|------|------|-----------|----------|--------|
| 🔵 Event | `PaymentInitiated` | — (내부) | — | 결제 준비 완료 기록 |

---

## UC-22. 결제 승인 (Confirm)

**🟠 Command:** `POST /api/v1/payments/confirm` — `ConfirmPayment`  
**Actor:** User

```
[User] ── ConfirmPayment(ConfirmPaymentRequest, entryAuthToken)
  │
  ▼
  Purchase Service: PurchaseConfirmCommandService.requestConfirm()
    ├─ Purchase 조회 (IN_PROGRESS 상태 확인)
    ├─ orderId, amount 검증 (변조 방지)
    └─ userId 소유권 검증
  │
  ▼
🔵 PaymentConfirmRequested
   └─ Event Sourcing: PurchaseEventStore append
  │
  ▼
  ├─ Toss Payments API 호출 (confirmPayment)
  │  paymentKey, orderId, amount 전송
  │
  ▼
🔵 PaymentConfirmSuccessEvent
   payload: purchaseId, paymentKey, orderId, amount, approvedAt
  │
  ▼
🟢 Policy: 티켓 생성 + 좌석 확정
   ├─ Redis에서 선택 좌석 정보 조회
   ├─ TicketGenerationService.generateTickets()
   │  └─ Purchase에 Ticket 추가
   ├─ Purchase.markAsCompleted()
   └─ DB에 Purchase + Ticket 저장
  │
  ▼
🔵 SeatPurchasedEvent (RabbitMQ)
   payload: eventId, seatIds, userId
  │
  ▼
🔵 PaymentCompletedEvent
   payload: purchaseId, eventId, userId, seatIds, amount, paymentMethod
  │
        ┌────────────────────┴────────────────────┐
        ▼                                         ▼
🟢 Policy: 좌석 매진 처리                  🟢 Policy: 구매 완료 알림
   Consumer: SeatPurchasedEventConsumer         Consumer: Notification
   └─ Seat 도메인에서 좌석 영구 매진            └─ Notification 도메인에 알림 생성
  │
  ▼
⬛ Read Model: ConfirmPaymentAcceptedResponse (202 Accepted)
```

| 구분 | 이름 | Transport | Consumer | Action |
|------|------|-----------|----------|--------|
| 🔵 Event | `PaymentConfirmRequested` | — (내부) | — | 결제 승인 요청 기록 (ES) |
| 🔵 Event | `PaymentConfirmSuccessEvent` | — (내부) | Purchase | 티켓 생성 트리거 |
| 🔵 Event | `SeatPurchasedEvent` | RabbitMQ | Seat | 좌석 매진 처리 |
| 🔵 Event | `PaymentCompletedEvent` | RabbitMQ | Seat, Notification | 매진 + 알림 트리거 |
| 🟢 Policy | 티켓 생성 + 좌석 확정 | — | Purchase | Ticket 생성 → DB 저장 |
| 🟢 Policy | 좌석 매진 처리 | — | SeatPurchasedEventConsumer | 좌석 available=false 영구 설정 |
| 🟢 Policy | 구매 완료 알림 | — | Notification | 사용자에게 구매 완료 알림 |

---

## UC-23. 결제 취소

**🟠 Command:** `POST /api/v1/payments/{paymentKey}/cancel` — `CancelPayment`  
**Actor:** User

```
[User] ── CancelPayment(paymentKey, CancelPaymentRequest)
  │
  ▼
  Purchase Service: PurchaseCancelService.cancelPayment()
    ├─ paymentKey로 Purchase 조회
    ├─ userId 소유권 검증
    └─ 취소 가능 상태 검증
  │
  ▼
  ├─ Toss Payments API 호출 (cancelPayment)
  │  paymentKey, cancelReason 전송
  │
  ▼
🔵 SeatPurchasedCanceledEvent
   payload: eventId, seatIds
  │
  ▼
🟢 Policy: 좌석 가용 복원
   Consumer: SeatPurchasedEventConsumer
   └─ Seat 도메인에서 좌석 available = true
  │
  ▼
🔵 RefundCompletedEvent
  │
  ▼
🟢 Policy: 환불 완료 알림 전송
   └─ Notification 도메인에 알림 생성
  │
  ▼
⬛ Read Model: CancelPaymentResponse
```

| 구분 | 이름 | Transport | Consumer | Action |
|------|------|-----------|----------|--------|
| 🔵 Event | `SeatPurchasedCanceledEvent` | RabbitMQ | Seat | 좌석 가용 복원 트리거 |
| 🔵 Event | `RefundCompletedEvent` | RabbitMQ | Notification | 환불 알림 트리거 |
| 🟢 Policy | 좌석 가용 복원 | — | SeatPurchasedEventConsumer | 좌석 available=true |
| 🟢 Policy | 환불 완료 알림 전송 | — | Notification | 사용자에게 환불 완료 알림 |

---

## UC-24. 구매 내역 조회

**🟠 Command:** `GET /api/v1/purchases/history` — `GetPurchaseHistory`  
**Actor:** User

```
[User] ── GetPurchaseHistory(statuses?, Pageable)
  │
  ▼
  Purchase Service: PurchaseViewRepository.findUserPurchaseList()
    ├─ LoggedInUserContext에서 userId 추출
    ├─ statuses 필터 (기본: DONE, EXPIRED)
    └─ Projection 조회 (N+1 방지)
  │
  ▼
⬛ Read Model: Page<PurchaseListProjection>
```

> 조회 전용 — 도메인 이벤트 없음.

---

## UC-25. 이벤트 구매 내역 조회 (Manager)

**🟠 Command:** `GET /api/v1/purchases/event/{eventId}` — `GetEventPurchases`  
**Actor:** Manager

```
[Manager] ── GetEventPurchases(eventId, status?, Pageable)
  │
  ▼
  Purchase Service: PurchaseViewRepository.findEventPurchaseList()
    ├─ eventId로 필터링
    ├─ status 필터 (기본: DONE)
    └─ Projection 조회
  │
  ▼
⬛ Read Model: Page<PurchaseListProjection>
```

> 조회 전용 — 도메인 이벤트 없음.

---

## UC-26. 내 환불 내역 조회

**🟠 Command:** `GET /api/v1/refunds/my` — `GetMyRefunds`  
**Actor:** User

```
[User] ── GetMyRefunds(userId, Pageable)
  │
  ▼
  Purchase Service: RefundQueryService.getUserRefundHistory()
    ├─ userId로 환불 내역 조회
    └─ 페이지네이션 적용
  │
  ▼
⬛ Read Model: Page<RefundDto>
```

> 조회 전용 — 도메인 이벤트 없음.

---

## UC-27. 관리자 단일 환불 처리

**🟠 Command:** `POST /api/v1/refunds/manager/single` — `ProcessManagerRefund`  
**Actor:** Manager

```
[Manager] ── ProcessManagerRefund(ManagerRefundRequest)
  │
  ▼
  Purchase Service: ManagerRefundService.processManagerRefund()
    ├─ purchaseId로 Purchase 조회
    └─ 환불 사유, 관리자 정보 설정
  │
  ▼
  ├─ Toss Payments API 호출 (cancelPayment)
  │
  ▼
🔵 ManagerRefundCompletedEvent
   payload: refundId, purchaseId, managerId
  │
  ▼
🔵 SeatPurchasedCanceledEvent
   └─ 좌석 가용 복원
  │
  ▼
🔵 RefundCompletedEvent
  │
  ▼
🟢 Policy: 좌석 가용 복원
   └─ Seat 도메인에서 좌석 available = true
  │
  ▼
🟢 Policy: 환불 완료 알림 전송
   └─ Notification 도메인에 알림 생성
  │
  ▼
⬛ Read Model: ManagerRefundResult (202 Accepted)
```

| 구분 | 이름 | Transport | Consumer | Action |
|------|------|-----------|----------|--------|
| 🔵 Event | `ManagerRefundCompletedEvent` | — (내부) | — | 관리자 환불 완료 기록 |
| 🔵 Event | `SeatPurchasedCanceledEvent` | RabbitMQ | Seat | 좌석 가용 복원 |
| 🔵 Event | `RefundCompletedEvent` | RabbitMQ | Notification | 환불 알림 트리거 |
| 🟢 Policy | 좌석 가용 복원 | — | Seat | 좌석 available=true |
| 🟢 Policy | 환불 완료 알림 전송 | — | Notification | 사용자에게 환불 완료 알림 |

---

## UC-28. 관리자 일괄 환불 처리

**🟠 Command:** `POST /api/v1/refunds/manager/batch` — `ProcessBatchRefund`  
**Actor:** Manager

```
[Manager] ── ProcessBatchRefund(BatchRefundRequest)
  │
  ▼
  Purchase Service: ManagerRefundService.processBatchRefund()
    ├─ eventId로 모든 Purchase 조회 (status = DONE)
    └─ 각 Purchase에 대해 반복:
        ├─ Toss Payments API 호출 (cancelPayment)
        ├─ ManagerRefundCompletedEvent 발행
        ├─ SeatPurchasedCanceledEvent 발행
        └─ RefundCompletedEvent 발행
  │
  ▼
🟢 Policy: 좌석 일괄 가용 복원
   └─ Seat 도메인에서 모든 좌석 available = true
  │
  ▼
🟢 Policy: 일괄 환불 알림 전송
   └─ Notification 도메인에 각 사용자별 알림
  │
  ▼
⬛ Read Model: List<ManagerRefundResult> (202 Accepted)
```

| 구분 | 이름 | Transport | Consumer | Action |
|------|------|-----------|----------|--------|
| 🔵 Event | `ManagerRefundCompletedEvent` (×N) | — (내부) | — | 각 환불 건 기록 |
| 🔵 Event | `SeatPurchasedCanceledEvent` (×N) | RabbitMQ | Seat | 좌석 가용 복원 |
| 🔵 Event | `RefundCompletedEvent` (×N) | RabbitMQ | Notification | 환불 알림 트리거 |
| 🟢 Policy | 좌석 일괄 가용 복원 | — | Seat | 모든 좌석 available=true |
| 🟢 Policy | 일괄 환불 알림 전송 | — | Notification | 각 사용자별 환불 알림 |

---

## UC-29. 알림 목록 조회

**🟠 Command:** `GET /api/v1/notifications` — `GetNotifications`  
**Actor:** User

```
[User] ── GetNotifications(Pageable)
  │
  ▼
  Notification Service: NotificationViewRepository.findUserNotificationList()
    ├─ LoggedInUserContext에서 userId 추출
    ├─ Projection 조회 (N+1 방지)
    └─ sentAt 기준 정렬
  │
  ▼
⬛ Read Model: Page<NotificationListProjection>
```

> 조회 전용 — 도메인 이벤트 없음.

---

## UC-30. 미읽은 알림 조회

**🟠 Command:** `GET /api/v1/notifications/unread` — `GetUnreadNotifications`  
**Actor:** User

```
[User] ── GetUnreadNotifications(Pageable)
  │
  ▼
  Notification Service: NotificationViewRepository.findUserUnreadNotificationList()
    ├─ userId + isRead = false 필터
    └─ Projection 조회
  │
  ▼
⬛ Read Model: Page<NotificationListProjection>
```

> 조회 전용 — 도메인 이벤트 없음.

---

## UC-31. 미읽은 알림 개수 조회

**🟠 Command:** `GET /api/v1/notifications/count/unread` — `GetUnreadCount`  
**Actor:** User

```
[User] ── GetUnreadCount
  │
  ▼
  Notification Service: NotificationViewRepository.countUnreadNotifications()
    └─ userId + isRead = false COUNT
  │
  ▼
⬛ Read Model: count (Long)
```

> 조회 전용 — 도메인 이벤트 없음.

---

## UC-32. 알림 상세 조회

**🟠 Command:** `GET /api/v1/notifications/{id}` — `GetNotificationDetail`  
**Actor:** User

```
[User] ── GetNotificationDetail(id)
  │
  ▼
  Notification Service: NotificationQueryService.getNotificationById()
    ├─ id로 알림 조회
    ├─ userId 소유권 검증
    └─ 읽음 처리 (isRead = true)
  │
  ▼
⬛ Read Model: NotificationDto
```

> 읽음 처리는 내부 상태 변경이지만 별도 도메인 이벤트 없음.

---

## UC-33. 알림 생성

**🟠 Command:** `POST /api/v1/notifications` — `CreateNotification`  
**Actor:** Admin / Manager

```
[Admin] ── CreateNotification(NotificationCreateRequestDto)
  │
  ▼
  Notification Service: NotificationCommandService.createNotification()
    ├─ userId, type, title, content, targetUrl 설정
    └─ Notification 엔티티 생성
  │
  ▼
🔵 NotificationCreated (도메인 이벤트)
  │
  ▼
🟢 Policy: SSE 실시간 푸시
   └─ NotificationEmitterService.createEmitter()
      └─ 구독 중인 Admin에게 실시간 알림 전송
  │
  ▼
⬛ Read Model: NotificationDto (201 Created)
```

| 구분 | 이름 | Transport | Consumer | Action |
|------|------|-----------|----------|--------|
| 🔵 Event | `NotificationCreated` | — (내부) | — | 알림 생성 완료 기록 |
| 🟢 Policy | SSE 실시간 푸시 | — | NotificationEmitterService | 구독자에게 실시간 알림 전송 |

---

## UC-34 ~ UC-36. 알림 삭제

**🟠 Commands:**
- `DELETE /api/v1/notifications/{id}` — `DeleteNotification`
- `POST /api/v1/notifications/batch-delete` — `BatchDeleteNotifications`
- `DELETE /api/v1/notifications/all` — `DeleteAllNotifications`

**Actor:** Admin

```
[Admin] ── DeleteNotification(id) / BatchDelete(ids) / DeleteAll
  │
  ▼
  Notification Service: NotificationCommandService
    ├─ id/ids로 알림 조회
    ├─ userId 소유권 검증
    └─ 삭제
  │
  ▼
  [Admin] ← 삭제 완료
```

> 이 유스케이스들은 도메인 이벤트 없이 동기적으로 완료됨.

---

## UC-37. 이미지 업로드

**🟠 Command:** `POST /api/v1/events/images` — `UploadImage`  
**Actor:** Manager

```
[Manager] ── UploadImage(MultipartFile)
  │
  ▼
  Event Service: ImageUploadService.upload()
    ├─ 파일 유효성 검증 (확장자, 크기)
    └─ 비동기 업로드 처리
  │
  ▼
⬛ Read Model: 이미지 URL
```

> 이 유스케이스는 도메인 이벤트 없이 비동기로 완료됨.

---

## UC-38. Payment Hold 생성 (Internal)

**🟠 Command:** `POST /api/v1/internal/events/payment-holds` — `CreatePaymentHold`  
**Actor:** System (Purchase → Event)

```
[Purchase] ── CreatePaymentHold(EventPaymentHoldCreateRequest)
  │
  ▼
  Event Service: EventPaymentHoldService.create()
    ├─ eventId 검증
    └─ PaymentHold 엔티티 생성
       (결제 중 이벤트 정보 변경 방지용 낙관적 락)
  │
  ▼
🔵 PaymentHoldCreated (도메인 이벤트)
  │
  ▼
⬛ Read Model: EventPaymentHoldCreateResponse
```

| 구분 | 이름 | Transport | Consumer | Action |
|------|------|-----------|----------|--------|
| 🔵 Event | `PaymentHoldCreated` | — (내부) | — | 결제 중 이벤트 변경 잠금 기록 |

---

## End-to-End Flow: 티켓 예매 전체 시나리오

```
┌─────────────────────────────────────────────────────────────────┐
│  UC-15: 대기열 진입                                              │
│  🟠 [User] → Broker: EnterWaitingQueue                           │
│     🔵 SSE: HEARTBEAT + QUEUE_POSITION (반복)                     │
└─────────────────────────────────────────────────────────────────┘
  │
  ▼
┌─────────────────────────────────────────────────────────────────┐
│  UC-17: 대기열 승격 (자동, System)                                │
│  🟠 Dispatcher: PromoteWaitingUsers (1초마다)                     │
│     🟢 Policy: Lua Script로 원자적 승격                           │
│     🔵 ENTRY Stream → DISPATCH Stream                            │
│     🟢 Policy: DISPATCH forwarding → Broker SSE                  │
│     🔵 [User] ← "구매 가능!" + EntryToken(JWT)                   │
└─────────────────────────────────────────────────────────────────┘
  │
  ▼
┌─────────────────────────────────────────────────────────────────┐
│  UC-18: 좌석 조회                                                │
│  🟠 [User] → Seat: GetSeatLayout                                 │
│     ⬛ ← SeatLayoutResponse                                      │
└─────────────────────────────────────────────────────────────────┘
  │
  ▼
┌─────────────────────────────────────────────────────────────────┐
│  UC-19: 좌석 선택                                                │
│  🟠 [User] → Seat: SelectSeat (EntryToken)                        │
│     🟢 Policy: Redis 분산 락 (5분 TTL)                           │
│     🔵 Seat Reserved                                              │
│     ⬛ ← SeatSelectResponse                                      │
└─────────────────────────────────────────────────────────────────┘
  │
  ▼
┌─────────────────────────────────────────────────────────────────┐
│  UC-21: 결제 준비                                                │
│  🟠 [User] → Purchase: InitiatePayment (EntryToken)               │
│     🔵 PaymentInitiated                                           │
│     ⬛ ← InitiatePaymentResponse → Toss PG 리다이렉트              │
└─────────────────────────────────────────────────────────────────┘
  │
  ▼
┌─────────────────────────────────────────────────────────────────┐
│  UC-22: 결제 승인                                                │
│  🟠 [User] → Purchase: ConfirmPayment (EntryToken)                │
│     🔵 PaymentConfirmRequested → ES append                        │
│     🔵 PaymentConfirmSuccessEvent                                 │
│     🟢 Policy: 티켓 생성 + 좌석 확정                               │
│     🔵 SeatPurchasedEvent (RabbitMQ) ──→ 🟢 좌석 매진             │
│     🔵 PaymentCompletedEvent (RabbitMQ) ──→ 🟢 구매 완료 알림      │
│     ⬛ ← 202 Accepted                                            │
└─────────────────────────────────────────────────────────────────┘
```

---

## Integration Event Matrix

| Source | 🔵 Event | Transport | 🟢 Policy / Consumer | Action |
|--------|----------|-----------|---------------------|--------|
| Auth | `SecurityUserRegisteredEvent` | RabbitMQ | User 프로필 자동 생성 | User 프로필 생성 |
| Auth | `SnsUserRegisteredEvent` | RabbitMQ | SNS User 프로필 자동 생성 | SNS User 프로필 생성 |
| User | `UserRegisteredEvent` | RabbitMQ | Auth에 userId 링크 | SecurityUser에 userId 연결 |
| User | `UserRegisteredFailedEvent` | RabbitMQ | Auth에 userId 링크 (실패) | SecurityUser 정리 (보상) |
| Event | `EventCreatedEvent` | RabbitMQ | Seat 레이아웃 연동 | SeatLayout 생성 |
| Event | `EventUpdatedEvent` | RabbitMQ | Seat 레이아웃 갱신 | SeatLayout 갱신 |
| Event | `EventUpdatedEvent` | RabbitMQ | 이벤트 변경 알림 | Notification 생성 |
| Event | `EventDeletedEvent` | RabbitMQ | 좌석 레이아웃 무효화 | 좌석 비활성화 |
| Purchase | `SeatPurchasedEvent` | RabbitMQ | 좌석 매진 처리 | 좌석 영구 매진 |
| Purchase | `SeatPurchasedCanceledEvent` | RabbitMQ | 좌석 가용 복원 | 좌석 available=true |
| Purchase | `PaymentCompletedEvent` | RabbitMQ | 좌석 매진 처리 | Seat 상태 업데이트 |
| Purchase | `PaymentCompletedEvent` | RabbitMQ | 구매 완료 알림 | Notification 생성 |
| Purchase | `RefundCompletedEvent` | RabbitMQ | 환불 완료 알림 전송 | Notification 생성 |
| Dispatcher | DISPATCH Stream | Redis Stream | EntryToken 발급 + SSE 알림 | Broker → User SSE 푸시 |
