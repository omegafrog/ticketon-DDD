# Exception / StatusCode 응답 리스트

최종 갱신: 2026-03-27

본 문서는 현재 코드 기준으로 **예외 계층**, **예외 → HTTP 상태코드 매핑**, **주요 API 성공 응답 statusCode**를 정리한 목록입니다.

---

## 1) 예외 계층 분리 현황

### 1.1 공통 예외 (platform/common)

| 분류 | 클래스 | 위치 | 비고 |
|---|---|---|---|
| Base | `BaseException` | `platform/common/src/main/java/org/codenbug/common/exception/BaseException.java` | code/message 기반 공통 베이스 |
| Common | `CommonException` | `platform/common/src/main/java/org/codenbug/common/exception/CommonException.java` | 공통 예외 계층 |
| Common | `AuthException` | `platform/common/src/main/java/org/codenbug/common/exception/AuthException.java` | 인증 계열 공통 예외 |
| Common | `JwtException` | `platform/common/src/main/java/org/codenbug/common/exception/JwtException.java` | JWT 계열 공통 예외 |
| Common | `ExpiredJwtException` | `platform/common/src/main/java/org/codenbug/common/exception/ExpiredJwtException.java` | 만료 토큰 |
| Common | `SignatureException` | `platform/common/src/main/java/org/codenbug/common/exception/SignatureException.java` | 서명 오류 |
| Common | `AccessDeniedException` | `platform/common/src/main/java/org/codenbug/common/exception/AccessDeniedException.java` | 접근 거부 |
| Common | `ControllerParameterValidationFailedException` | `platform/common/src/main/java/org/codenbug/common/exception/ControllerParameterValidationFailedException.java` | 컨트롤러 파라미터 검증 실패 |

### 1.2 도메인 예외 (서비스별)

| 서비스 | 클래스 | 위치 | 기본 코드 |
|---|---|---|---|
| event | `PaymentHoldRejectedException` | `event/src/main/java/org/codenbug/event/domain/PaymentHoldRejectedException.java` | `409` |
| purchase | `EventChangeDetectedException` | `purchase/src/main/java/org/codenbug/purchase/domain/EventChangeDetectedException.java` | `409` |
| seat | `ConflictException` | `seat/src/main/java/org/codenbug/seat/global/exception/ConflictException.java` | `409` |
| auth | `UserValidationException` | `auth/src/main/java/org/codenbug/auth/global/UserValidationException.java` | `status.value()` 사용 |

---

## 2) 예외 핸들러/어드바이저와 상태코드 매핑

### 2.1 App 공통 ExceptionAdvisor

파일: `app/src/main/java/org/codenbug/app/ui/ExceptionAdvisor.java`

| 예외 | HTTP | RsData code | 메시지 |
|---|---:|---:|---|
| `MethodArgumentNotValidException`, `BindException` | 400 | `400` | 요청 값 검증에 실패했습니다. |
| `ConstraintViolationException` | 400 | `400` | 요청 값 검증에 실패했습니다. |
| `HttpMessageNotReadableException` | 400 | `400` | 요청 본문 형식이 올바르지 않습니다. |
| `BaseException` | `ex.code` 기반 | `ex.code` | `ex.message` |
| `Exception` | 500 | `500` | 서버 내부 오류가 발생했습니다. |

적용 범위(basePackages): `org.codenbug.user`, `org.codenbug.event`, `org.codenbug.purchase`, `org.codenbug.notification`, `org.codenbug.app`

### 2.2 Auth GlobalExceptionHandler

파일: `auth/src/main/java/org/codenbug/auth/global/GlobalExceptionHandler.java`

| 예외 | HTTP | RsData code |
|---|---:|---:|
| `MethodArgumentNotValidException`, `BindException` | 400 | `400` |
| `HttpMessageNotReadableException` | 400 | `400` |
| `EntityNotFoundException`, `AccessDeniedException` | 401 | `401` |
| `AsyncRequestTimeoutException` | 408 | `408` |
| `DataIntegrityViolationException` | 400 | `400` |
| `UserValidationException` | `e.getStatus()` | 전달받은 RsData 사용 |
| `Exception` | 500 | `500` |

### 2.3 Event GlobalExceptionHandler

파일: `event/src/main/java/org/codenbug/event/ui/GlobalExceptionHandler.java`

| 예외 | HTTP | RsData code |
|---|---:|---:|
| validation 계열 (`MethodArgumentNotValidException`, `ControllerParameterValidationFailedException`, `BindException`) | 400 | `400` |
| `HttpMessageNotReadableException` | 400 | `400` |
| `IllegalStateException` | 400 | `400` |
| `EntityNotFoundException`, `JpaObjectRetrievalFailureException` | 404 | `404` |
| `Exception` | 500 | `500` |

### 2.4 SecurityAOP ExceptionHandler

파일: `security-aop/src/main/java/org/codenbug/securityaop/aop/SecurityAopExceptionHandler.java`

| 예외 | HTTP | RsData code |
|---|---:|---:|
| `IllegalArgumentException`, JWT 예외 | 401 | `401` |
| `AccessDeniedException` | 403 | `403` |

### 2.5 Broker GlobalExceptionHandler

파일: `broker/src/main/java/org/codenbug/broker/infra/GlobalExceptionHandler.java`

| 예외 | HTTP | RsData code |
|---|---:|---:|
| `Exception` | 500 | `500` |
| `RuntimeException` | 500 | `500` |
| `IllegalStateException` | 409 | `409` |

---

## 3) 주요 API 성공 응답 statusCode 매트릭스

### 3.1 생성/비동기 수락 성격 API

| API | HTTP | RsData code |
|---|---:|---:|
| `POST /api/v1/auth/register` | 202 | `202` |
| `POST /api/v1/events` | 201 | `201` |
| `POST /api/v1/events/{eventId}/payment-holds` (internal) | 201 | `201` |
| `POST /api/v1/payments/init` | 201 | `201` |
| `POST /api/v1/payments/confirm` | 202 | `202` |
| `POST /api/v1/refunds/manager/single` | 202 | `202` |
| `POST /api/v1/refunds/manager/batch` | 202 | `202` |
| `POST /api/v1/notifications` | 201 | `201` |

### 3.2 일반 조회/수정/삭제 API

기본 성공 응답은 HTTP 200 / RsData code `200`.

대표 예시:

- User: `/api/v1/users/me`, `/api/v1/users/me (PUT)`
- Event: 조회/수정/삭제/상태변경, 카테고리 조회, 캐시 테스트
- Seat: 좌석 조회/선택/취소, internal seat-layout 조회
- Purchase: confirm status 조회, cancel, query 계열
- Refund: 내역/상세/상태별 조회
- Notification: 조회/삭제/count/test
- Broker: polling/monitoring, disconnect

### 3.3 컨트롤러 내 명시 예외 응답

| API | HTTP | RsData code | 조건 |
|---|---:|---:|---|
| `POST /internal/events/{eventId}/payment-holds` | 409 | `409` | `PaymentHoldRejectedException` |
| `GET /internal/events/{eventId}/version-check` | 400 | `400` | invalid `status` param |
| `GET /api/v1/auth/social/{type}/callback` | 500 | `500` | social login 처리 실패 |
| `POST /api/v1/auth/refresh` | 401 | `401` | refresh 실패 |

---

## 4) 비고

- SSE/파일 스트리밍 응답(`SseEmitter`, `Resource`)은 응답 바디 특성상 `RsData` 래핑 대상이 아닐 수 있음.
- 본 문서는 현재 코드 스냅샷 기준이며, status code 정책 변경 시 함께 갱신 필요.
