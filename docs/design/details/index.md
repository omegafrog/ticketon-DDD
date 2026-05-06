# DDD 설계 인덱스

## 1. 문서 링크
- 도메인 모델: docs/design/details/도메인모델.md
- 어그리거트: docs/design/details/어그리거트.md
- 애플리케이션 서비스: docs/design/details/애플리케이션서비스.md
- 바운디드 컨텍스트: docs/design/details/바운디드컨텍스트.md

## 2. 전체 구성 요약
- 설계 입력: docs/design/요구사항.md, docs/design/유스케이스.md, docs/design/이벤트 스토밍.md와 승인된 상세 설계 문서 4종을 기준으로 한다.
- 핵심 도메인 흐름: 회원/소셜/관리자 인증, 사용자 프로필, 공개 이벤트 탐색, 매니저 이벤트 운영과 이미지 처리, polling 대기열과 입장 토큰, 좌석 점유, 예매 생성, 결제 승인, 구매/환불 조회, 매니저 환불, DLQ 재처리, 감사/운영 관찰이다.
- 주요 경계: 인증/인가 BC와 사용자 정보 BC는 분리한다. Event operation, public discover, event image는 Event BC로 병합한다. Reservation, purchase/payment, refund, DLQ reprocessing은 Booking & Commerce BC로 병합한다. 좌석 재고, 대기열/입장, 관리자 백오피스, 감사/운영 관찰은 변경 속도와 일관성 책임이 달라 별도 BC로 유지한다.
- 전역 정책: 관리자 백오피스는 session auth, 30분 비활성 만료, 3회 실패 계정 잠금을 사용한다. 모든 API는 p95 1초 미만과 API별 100 TPS를 목표로 관찰한다. 사용자 노출 오류는 사용자 오류/서버 오류로 제한한다. 암호화 대상은 비밀번호만이며, 인증/인가 보호 API 감사 로그와 운영 로그는 1개월 보존한다. 이미지 업로드/교체는 3회 재시도 후 대체 이미지를 노출하고 실패 알림은 지연 처리한다.

## 3. 컴포넌트 카탈로그
### 3.1 바운디드 컨텍스트
|BC|책임|주요 컴포넌트|
|---|---|---|
|인증/인가 BC|회원가입, 이메일/소셜 로그인, 토큰 발급/재발급, 로그아웃, 보호 API 접근 판단, 계정 상태/역할 관리|UserAccountAggregate, RefreshTokenSessionAggregate, UserAccount, SocialAccountLink, RefreshTokenSession, AccountAccessApplicationService|
|사용자 정보 BC|본인 프로필 조회/수정, 관리자 조회용 사용자 표시 정보와 마스킹 값 관리|UserProfile, ProfileName, PhoneNumber, Region, MaskedUserIdentity, UserProfileApplicationService|
|관리자 백오피스 BC|관리자 세션 로그인, 30분 비활성 만료, 3회 실패 잠금 요청, 사용자 운영 업무 조정|AdminBackofficeSessionAggregate, AdminBackofficeSession, AdminLoginFailureRecord, AdminBackofficeApplicationService|
|Event BC|공개 이벤트 탐색, 매니저 이벤트 운영, 이벤트 이미지 업로드/교체, 이미지 3회 재시도/fallback, 이벤트 버전 충돌 기준 제공|EventAggregate, EventCategoryAggregate, EventImageAggregate, PublicEventQueryApplicationService, EventOperationApplicationService, EventImageApplicationService|
|대기열/입장 BC|polling 대기열 진입/조회/이탈, 다음 회원 승급, 입장 토큰 발급/만료, 대기열 지표 관찰|QueueEntryAggregate, AdmissionTokenAggregate, QueueApplicationService|
|좌석 재고 BC|좌석 배치 조회, 좌석 점유/판매 확정/해제, 동일 좌석 중복 판매 방지|SeatInventoryAggregate, SeatLayout, Seat, SeatInventoryPort|
|Booking & Commerce BC|예매 생성/만료/취소/확정, 결제 준비/승인/상태 조회, 구매 내역, 환불, 결제/환불 DLQ 재처리|ReservationAggregate, PurchaseAggregate, RefundAggregate, DeadLetterTaskAggregate, ReservationApplicationService, PaymentApplicationService, RefundApplicationService, DeadLetterReprocessApplicationService|
|감사/운영 관찰 BC|보호 API 감사 로그, 관리자 변경 감사 로그, 운영 로그, p95/TPS 관찰, 1개월 보존 정책 관리|AuditLogAggregate, OperationalLogAggregate, AuditOperationalApplicationService|

### 3.2 어그리거트
|어그리거트|루트|소속 BC|핵심 불변식|
|---|---|---|---|
|UserAccountAggregate|UserAccount|인증/인가 BC, 사용자 정보 BC 협력|일반 가입은 USER 역할, 정지/삭제/잠금 계정은 보호 기능 접근 불가, 관리자 3회 실패 시 잠금|
|RefreshTokenSessionAggregate|RefreshTokenSession|인증/인가 BC|로그아웃으로 무효화된 refresh token은 재사용 불가|
|AdminBackofficeSessionAggregate|AdminBackofficeSession|관리자 백오피스 BC|관리자 session은 30분 비활성 상태이면 만료|
|EventAggregate|Event|Event BC|OPEN/기간/공연 시작 전일 때만 예매 가능, DELETED 미노출, 구매 제약 변경은 eventVersion 충돌 기준|
|EventCategoryAggregate|EventCategory|Event BC|활성 카테고리만 이벤트 탐색 조건에 사용|
|EventImageAggregate|EventImage|Event BC|webp 및 10MB 제한, 저장/교체 실패 3회 재시도 후 fallback image 노출과 지연 알림 예약|
|QueueEntryAggregate|QueueEntry|대기열/입장 BC|대기열 재접속 금지, WAITING/승급 후보 상태에서만 이탈/승급|
|AdmissionTokenAggregate|AdmissionToken|대기열/입장 BC|승급 회원에게만 발급, 결제 완료/결제 제한시간 경과 시 만료|
|SeatInventoryAggregate|SeatLayout|좌석 재고 BC|선택 좌석 묶음은 원자적으로 AVAILABLE 검증 후 HELD/SOLD/AVAILABLE 전이|
|ReservationAggregate|Reservation|Booking & Commerce BC|결제 대기 예매는 1시간 제한, 회원당 결제 대기 1개, 이벤트 버전 변경 시 롤백/복구 대상|
|PurchaseAggregate|Purchase|Booking & Commerce BC|결제 금액과 좌석 총액 일치, 비동기 승인 결과 또는 만료 보상으로만 상태 전이|
|RefundAggregate|Refund|Booking & Commerce BC|부분 환불 금지, REQUESTED -> PROCESSING -> COMPLETED/FAILED 전이|
|DeadLetterTaskAggregate|DeadLetterTask|Booking & Commerce BC|결제/환불 실패 작업은 재처리 가능한 유형과 대상 식별자를 가져야 함|
|AuditLogAggregate|AuditLogEntry|감사/운영 관찰 BC|보호 API와 관리자 변경은 요청/응답/에러 원문, 사용자, 발생 시각, 1개월 보존 포함|
|OperationalLogAggregate|OperationalLogEntry|감사/운영 관찰 BC|운영 로그는 1개월 보존, p95 1초 미만/API별 100 TPS 관찰값 기록 가능|

### 3.3 엔티티
|엔티티|소속 어그리거트/BC|식별 기준|
|---|---|---|
|UserAccount|UserAccountAggregate / 인증/인가 BC|userId|
|UserProfile|사용자 정보 BC|profileId 또는 userId|
|SocialAccountLink|UserAccountAggregate / 인증/인가 BC|socialLinkId|
|RefreshTokenSession|RefreshTokenSessionAggregate / 인증/인가 BC|refreshTokenId|
|AdminBackofficeSession|AdminBackofficeSessionAggregate / 관리자 백오피스 BC|adminSessionId|
|AdminLoginFailureRecord|UserAccountAggregate / 관리자 백오피스 BC 협력|adminLoginFailureId 또는 userId|
|Event|EventAggregate / Event BC|eventId|
|EventCategory|EventCategoryAggregate / Event BC|categoryId|
|EventImage|EventImageAggregate / Event BC|imageId|
|QueueEntry|QueueEntryAggregate / 대기열/입장 BC|queueEntryId|
|AdmissionToken|AdmissionTokenAggregate / 대기열/입장 BC|admissionTokenId|
|SeatLayout|SeatInventoryAggregate / 좌석 재고 BC|seatLayoutId 또는 eventId|
|Seat|SeatInventoryAggregate / 좌석 재고 BC|seatId|
|Reservation|ReservationAggregate / Booking & Commerce BC|reservationId|
|Purchase|PurchaseAggregate / Booking & Commerce BC|purchaseId|
|Refund|RefundAggregate / Booking & Commerce BC|refundId|
|DeadLetterTask|DeadLetterTaskAggregate / Booking & Commerce BC|deadLetterTaskId|
|AuditLogEntry|AuditLogAggregate / 감사/운영 관찰 BC|auditLogId|
|OperationalLogEntry|OperationalLogAggregate / 감사/운영 관찰 BC|operationalLogId|

### 3.4 값 객체
|VO|사용 위치|핵심 검증 규칙|
|---|---|---|
|EmailAddress, PasswordHash, OAuthProviderIdentity|UserAccount, SocialAccountLink|이메일 형식, 비밀번호만 암호화/해시 저장, 지원 OAuth provider|
|UserRole, RoleSet, AccountStatus, AdminLockStatus|UserAccount|역할 집합 필수, 정지/삭제/잠금 보호 기능 차단|
|AdminLoginFailureCount, BackofficeSessionLifetime|AdminLoginFailureRecord, AdminBackofficeSession|실패 횟수 3회 잠금, session 30분 비활성 만료|
|ProfileName, Age, Gender, PhoneNumber, Region, MaskedUserIdentity|UserProfile, 관리자 조회 응답|프로필 값 형식 검증, 이메일/이름 마스킹 규칙|
|EventTitle, EventDescription, BookingPeriod, PerformanceStartAt, EventStatus, EventVersion, Price, EventSearchCondition|Event, Event 조회|예매 기간은 공연 시작일 전, OPEN 노출/DELETED 미노출, version 비교 가능|
|ImageFileSpec, StaticImagePath, ImageRetryPolicy, FallbackImageRef|EventImage|webp, 10MB 이하, 최대 재시도 3회, fallback image 참조 유효|
|QueuePosition, QueueStatus, AdmissionTokenValue, AdmissionTokenStatus|QueueEntry, AdmissionToken|순번 1 이상, 허용 상태 전이, active token만 예매/결제 가능|
|SeatIdList, TicketQuantity, SeatStatus|Reservation, SeatInventory|좌석 목록 비어 있지 않음, 중복 없음, 티켓 수량 일치, AVAILABLE/HELD/SOLD 전이|
|ReservationStatus, PaymentDeadline|Reservation|PAYMENT_PENDING/CONFIRMED/EXPIRED/CANCELED, 결제 제한시간 1시간|
|Money, PaymentProvider, PaymentKey, OrderId, PurchaseStatus|Purchase, Refund|금액 0 이상, Toss Payments, 주문/결제키 유효, 결제 상태 전이 제한|
|RefundStatus, RefundReason|Refund|전체 환불만 허용, 환불 사유 필수, REQUESTED/PROCESSING/COMPLETED/FAILED|
|DeadLetterReason, RetryState|DeadLetterTask|재처리 대상 실패 사유와 음수 불가 재시도 상태|
|UserFacingErrorLevel, AuditPayload, LogRetentionPeriod, ApiPerformanceTarget|감사/운영 관찰 BC|사용자 오류/서버 오류만 노출, 원문/사용자/시간 포함, 1개월 보존, p95 1초 미만과 API별 100 TPS|

### 3.5 도메인 서비스
|도메인 서비스|책임|호출 주체|관련 유스케이스|
|---|---|---|---|
|UserUniquenessService, CredentialVerificationService, SocialAccountIdentificationService, ProtectedAccessPolicyService|계정 중복, 자격 증명, 소셜 식별, 보호 기능 접근 판단|AccountAccessApplicationService, UserProfileApplicationService, AdminBackofficeApplicationService|UC-01, UC-02, UC-04, UC-05, UC-22~UC-29|
|AdminBackofficeAccessService, UserMaskingService|관리자 session/권한/잠금과 사용자 마스킹 판단|AdminBackofficeApplicationService|UC-22~UC-28|
|EventAuthorAuthorizationService, EventBookabilityService, EventChangeConflictService|이벤트 작성자 권한, 예매 가능성, eventVersion 충돌 판단|EventOperationApplicationService, QueueApplicationService, ReservationApplicationService, PaymentApplicationService, RefundApplicationService|UC-06, UC-08, UC-09, UC-15~UC-21|
|ImageUploadPolicyService, ImageFileValidationService, ImageStorageRetryService|이미지 업로드/교체 가능성, 파일 조건, 3회 재시도/fallback 판단|EventImageApplicationService|UC-18, UC-19|
|QueuePromotionService, PollingRateControlPolicyService, AdmissionTokenValidationService|대기열 승급, polling rate control, admission token 유효성 판단|QueueApplicationService, ReservationApplicationService, PaymentApplicationService|UC-06~UC-09, UC-13|
|SeatAvailabilityService, PendingReservationLimitService, ReservationExpirationService|좌석 점유 가능성, 회원당 결제 대기 1개, 예매 만료 정리 판단|ReservationApplicationService, ReservationExpirationApplicationService|UC-08, UC-09, UC-13|
|PaymentAmountVerificationService, PaymentApprovalPolicyService, PaymentExpirationCompensationService|결제 금액 검증, 비동기 승인 결과 해석, 예매 만료 중 결제 취소 판단|PaymentApplicationService, ReservationExpirationApplicationService|UC-09, UC-10|
|PurchaseQueryAuthorizationService, RefundEligibilityService|구매/환불 조회 권한, 전체 환불 가능성 판단|PaymentApplicationService, RefundApplicationService|UC-10~UC-12, UC-20, UC-21|
|DeadLetterRegistrationService, DeadLetterReprocessPolicyService|결제/환불 실패 DLQ 등록과 재처리 가능성 판단|PaymentApplicationService, RefundApplicationService, DeadLetterReprocessApplicationService|UC-09, UC-21|
|AuditCapturePolicyService, OperationalLoggingPolicyService, UserFacingErrorClassificationService, ApiPerformancePolicyService|감사 캡처, 운영 로그, 사용자 오류 축약, p95/TPS 관찰 정책|모든 보호 API 애플리케이션 서비스, AuditOperationalApplicationService|UC-04~UC-29, 전역 NFR|

### 3.6 애플리케이션 서비스
|애플리케이션 서비스|소속 BC|오케스트레이션 유스케이스|호출 컴포넌트|비즈니스 로직 노출 방지 규칙|
|---|---|---|---|---|
|AccountAccessApplicationService|인증/인가 BC|UC-01, UC-02, UC-29|UserAccountAggregate, RefreshTokenSessionAggregate, OAuthVerificationPort, TokenIssuerPort, TokenCookiePort|자격 증명, 계정 상태, refresh token 무효화 판단은 domain/root에 둔다.|
|UserProfileApplicationService|사용자 정보 BC|UC-04, UC-05|UserAccountAggregate, UserProfileQueryRepository, AuditLogPort|본인/보호 접근 판단과 프로필 VO 검증은 domain/root에 둔다.|
|PublicEventQueryApplicationService|Event BC|UC-03|EventAggregate, EventSearchCondition, ViewCountIncrementPort|OPEN/DELETED 노출 규칙과 오류 축약은 domain policy 결과를 사용한다.|
|EventOperationApplicationService|Event BC|UC-14~UC-17|EventAggregate, SeatLayoutProvisionPort, PaymentConflictPort|작성자 권한, 구매 제약 변경, 충돌 판단은 domain service/root에 둔다.|
|EventImageApplicationService|Event BC|UC-18, UC-19|EventImageAggregate, ImageStoragePort, ImageStorageRetryService|webp/10MB/3회 재시도/fallback/지연 알림 판단은 domain/root에 둔다.|
|QueueApplicationService|대기열/입장 BC|UC-06, UC-07|QueueEntryAggregate, AdmissionTokenAggregate, EventBookabilityPort, QueueMetricsPort|슬롯, 재접속, rate control 판단은 domain service가 수행한다.|
|ReservationApplicationService|Booking & Commerce BC|UC-08, UC-13|ReservationAggregate, SeatInventoryPort, AdmissionTokenPort, EventSnapshotPort|회원당 결제 대기 1개, 좌석 중복 판매, eventVersion 충돌, token 유효성은 domain/root가 판단한다.|
|ReservationExpirationApplicationService|Booking & Commerce BC|UC-08, UC-09, UC-13|ReservationAggregate, PurchaseAggregate, SeatInventoryPort, AdmissionTokenPort, PaymentCancelPort|예매 만료와 결제 중 만료 보상은 root 멱등 전이와 포트 결과만 사용한다.|
|PaymentApplicationService|Booking & Commerce BC|UC-09, UC-10, UC-11|PurchaseAggregate, ReservationAggregate, SeatInventoryPort, TossPaymentPort, PaymentApprovalPublisher, DLQPort|금액 일치, 만료 보상, eventVersion 충돌, DLQ 등록은 domain/root에 둔다.|
|RefundApplicationService|Booking & Commerce BC|UC-12, UC-20, UC-21|RefundAggregate, DeadLetterTaskAggregate, TossCancelPort, RefundProcessingPublisher, DLQPort|부분 환불 금지와 작성자 권한은 domain service/root가 판단한다.|
|AdminBackofficeApplicationService|관리자 백오피스 BC|UC-22~UC-28|UserAccountAggregate, AdminBackofficeSessionAggregate, BackofficeSessionPort, AuditLogPort|3회 잠금, 30분 만료, 정지/삭제 전이는 root/domain service가 처리한다.|
|AuditOperationalApplicationService|감사/운영 관찰 BC|UC-04~UC-29, 전역 NFR|AuditLogAggregate, OperationalLogAggregate, RequestResponseCapturePort, MetricsObservationPort|감사 payload, 1개월 보존, p95/TPS 관찰값 검증은 로그 aggregate가 수행한다.|
|DeadLetterReprocessApplicationService|Booking & Commerce BC|UC-09, UC-21|DeadLetterTaskAggregate, PurchaseAggregate, RefundAggregate, TossPaymentPort, TossCancelPort, DLQPort|재처리 가능 상태와 대상 도메인 상태 판단은 domain service가 수행한다.|

### 3.7 포트/외부 협력 후보
|포트/협력 객체|종류|호출 주체|목적|관련 유스케이스|
|---|---|---|---|---|
|OAuthVerificationPort|외부 시스템 포트|AccountAccessApplicationService|Google/Kakao OAuth 검증|UC-02|
|TokenIssuerPort, TokenCookiePort|인프라 포트|AccountAccessApplicationService|액세스/리프레시 토큰 발급, 로그아웃 쿠키 종료|UC-02, UC-29|
|BackofficeSessionPort|인프라 포트|AdminBackofficeApplicationService|관리자 session 식별자와 만료 정보 연계|UC-22~UC-28|
|EventBookabilityPort, EventSnapshotPort, EventOwnershipPort|다른 BC client port|QueueApplicationService, ReservationApplicationService, PaymentApplicationService, RefundApplicationService|이벤트 예매 가능성, 커밋 직전 eventVersion, 작성자 권한 확인|UC-06, UC-08, UC-09, UC-20, UC-21|
|PaymentConflictPort|다른 BC command port|EventOperationApplicationService|이벤트 수정 중 진행 결제 중단과 예매 대기 복구 요청|UC-15|
|SeatInventoryPort|다른 BC client/command port|ReservationApplicationService, PaymentApplicationService, ReservationExpirationApplicationService|좌석 조회, 점유, 판매 확정, 해제|UC-08, UC-09, UC-13|
|AdmissionTokenPort|다른 BC client/command port|ReservationApplicationService, PaymentApplicationService, ReservationExpirationApplicationService|입장 토큰 검증/만료/정리|UC-08, UC-09, UC-13|
|TossPaymentPort, PaymentCancelPort, TossCancelPort|외부 결제 포트|PaymentApplicationService, ReservationExpirationApplicationService, RefundApplicationService, DeadLetterReprocessApplicationService|결제 승인, 예매 만료 중 결제 취소, 전체 환불/취소|UC-09, UC-21|
|PaymentApprovalPublisher, RefundProcessingPublisher, DLQPort|메시징/복구 포트|PaymentApplicationService, RefundApplicationService, DeadLetterReprocessApplicationService|결제/환불 비동기 처리, 실패 작업 DLQ 적재와 재처리|UC-09, UC-21|
|ImageStoragePort|외부/파일 저장 포트|EventImageApplicationService|이미지 업로드 URL, 저장/삭제/정적 경로, 실패 결과 전달|UC-18, UC-19|
|ViewCountIncrementPort, QueueMetricsPort|비동기/운영 포트|PublicEventQueryApplicationService, QueueApplicationService|조회수 증가, 대기열 사용자 수/polling 요청률/활성 토큰 관찰|UC-03, UC-06, UC-07|
|AuditLogPort, OperationalObservationPort|감사/운영 포트|모든 보호 API 애플리케이션 서비스|보호 API 감사 로그, 관리자 변경 감사 로그, 운영 로그, p95/TPS 지표 기록|UC-04~UC-29, 전역 NFR|
|RequestResponseCapturePort, MetricsObservationPort|인프라/운영 포트|AuditOperationalApplicationService|요청/응답/에러 원문 캡처, 성능 관찰값 수집|UC-04~UC-29, 전역 NFR|

## 4. 유스케이스별 커뮤니케이션
|유스케이스|애플리케이션 서비스|참여 어그리거트/도메인 서비스|BC 간 커뮤니케이션|외부 협력|비고|
|---|---|---|---|---|---|
|UC-01|AccountAccessApplicationService|UserAccountAggregate, UserUniquenessService|없음|AuditLogPort, OperationalObservationPort|일반 회원가입은 USER 역할과 password-only encryption을 적용한다.|
|UC-02|AccountAccessApplicationService|UserAccountAggregate, RefreshTokenSessionAggregate, CredentialVerificationService, SocialAccountIdentificationService|없음|OAuthVerificationPort, TokenIssuerPort, AuditLogPort|소셜 최초 가입/연동은 이메일 기준이다.|
|UC-03|PublicEventQueryApplicationService|EventAggregate, EventSearchCondition, UserFacingErrorClassificationService|Event BC 내부 조회|ViewCountIncrementPort, OperationalObservationPort|public discover는 Event BC 내부 책임이다.|
|UC-04, UC-05|UserProfileApplicationService|UserAccountAggregate, ProtectedAccessPolicyService, AuditCapturePolicyService|사용자 정보 BC -> 인증/인가 BC 접근 판단|AuditLogPort, OperationalObservationPort|보호 API 감사 로그는 1개월 보존한다.|
|UC-06, UC-07|QueueApplicationService|QueueEntryAggregate, AdmissionTokenAggregate, QueuePromotionService, PollingRateControlPolicyService|대기열/입장 BC -> Event BC 예매 가능성 확인|EventBookabilityPort, QueueMetricsPort, AuditLogPort|공식 경로는 HTTP polling이다.|
|UC-08|ReservationApplicationService|ReservationAggregate, SeatAvailabilityService, PendingReservationLimitService, EventChangeConflictService|Booking & Commerce BC -> 대기열/입장 BC, Event BC, 좌석 재고 BC|AdmissionTokenPort, EventSnapshotPort, SeatInventoryPort, AuditLogPort|유효 입장 토큰, 좌석 점유, eventVersion 재확인 후 예매를 생성한다.|
|UC-09|PaymentApplicationService, ReservationExpirationApplicationService, DeadLetterReprocessApplicationService|PurchaseAggregate, ReservationAggregate, DeadLetterTaskAggregate, PaymentAmountVerificationService, PaymentApprovalPolicyService, PaymentExpirationCompensationService|Booking & Commerce BC -> Event BC, 좌석 재고 BC, 대기열/입장 BC|TossPaymentPort, PaymentCancelPort, PaymentApprovalPublisher, DLQPort|결제 승인 중 예매 만료 시 결제를 취소하고, 실패 작업은 DLQ로 재처리한다.|
|UC-10, UC-11|PaymentApplicationService|PurchaseAggregate, PurchaseQueryAuthorizationService, ApiPerformancePolicyService|필요 시 인증/인가 BC 접근 판단|AuditLogPort, OperationalObservationPort|결제 상태와 구매 내역은 p95/TPS 관찰 대상이다.|
|UC-12|RefundApplicationService|RefundAggregate, RefundEligibilityService|필요 시 인증/인가 BC 접근 판단|AuditLogPort, OperationalObservationPort|회원 환불 목록/상세 조회 권한을 확인한다.|
|UC-13|ReservationApplicationService, ReservationExpirationApplicationService|ReservationAggregate, SeatInventoryAggregate, AdmissionTokenAggregate, ReservationExpirationService|Booking & Commerce BC -> 좌석 재고 BC, 대기열/입장 BC|SeatInventoryPort, AdmissionTokenPort, AuditLogPort|결제 전 포기는 좌석/토큰 정리와 멱등 처리 대상이다.|
|UC-14|EventOperationApplicationService|EventAggregate, ProtectedAccessPolicyService|Event BC -> 좌석 재고 BC 좌석 레이아웃 생성 요청|SeatLayoutProvisionPort, AuditLogPort, OperationalObservationPort|매니저 이벤트 등록 후 좌석 재고 모델을 생성한다.|
|UC-15|EventOperationApplicationService|EventAggregate, EventAuthorAuthorizationService, EventChangeConflictService|Event BC -> Booking & Commerce BC 진행 결제 중단/예매 대기 복구 요청|PaymentConflictPort, AuditLogPort, OperationalObservationPort|구매 제약 변경은 eventVersion 충돌 기준이 된다.|
|UC-16, UC-17|EventOperationApplicationService|EventAggregate, EventAuthorAuthorizationService|없음|AuditLogPort, OperationalObservationPort|상태 변경/삭제는 Event BC 내부 root 전이로 처리한다.|
|UC-18, UC-19|EventImageApplicationService|EventImageAggregate, ImageUploadPolicyService, ImageFileValidationService, ImageStorageRetryService|Event BC 내부 이미지 모델 변경|ImageStoragePort, AuditLogPort, OperationalObservationPort|3회 재시도 후 fallback image와 지연 실패 알림을 예약한다.|
|UC-20|RefundApplicationService|EventAuthorAuthorizationService, PurchaseQueryAuthorizationService|Booking & Commerce BC -> Event BC 작성자 권한 확인|EventOwnershipPort, AuditLogPort, OperationalObservationPort|매니저는 자신이 생성한 이벤트 구매 내역만 조회한다.|
|UC-21|RefundApplicationService, DeadLetterReprocessApplicationService|RefundAggregate, DeadLetterTaskAggregate, RefundEligibilityService, DeadLetterRegistrationService|Booking & Commerce BC -> Event BC 작성자 권한 확인|EventOwnershipPort, TossCancelPort, RefundProcessingPublisher, DLQPort|부분 환불은 금지하며 환불 실패는 DLQ 재처리 대상이다.|
|UC-22|AdminBackofficeApplicationService|UserAccountAggregate, AdminBackofficeSessionAggregate, AdminBackofficeAccessService|관리자 백오피스 BC -> 인증/인가 BC 계정 잠금 요청|BackofficeSessionPort, AuditLogPort, OperationalObservationPort|session auth, 30분 비활성 만료, 3회 실패 잠금을 적용한다.|
|UC-23|AdminBackofficeApplicationService|AdminBackofficeSessionAggregate, UserMaskingService|관리자 백오피스 BC -> 사용자 정보 BC 마스킹 조회|BackofficeSessionPort, AuditLogPort, OperationalObservationPort|사용자 목록/상세는 마스킹 값으로 반환한다.|
|UC-24~UC-28|AdminBackofficeApplicationService|UserAccountAggregate, AdminBackofficeSessionAggregate, AdminBackofficeAccessService|관리자 백오피스 BC -> 인증/인가 BC 계정 생성/승급/삭제/정지/해제 명령|BackofficeSessionPort, AuditLogPort, OperationalObservationPort|관리자 변경 감사 로그는 1개월 보존한다.|
|UC-29|AccountAccessApplicationService|RefreshTokenSessionAggregate, AuditCapturePolicyService|없음|TokenCookiePort, AuditLogPort, OperationalObservationPort|refresh token session root의 invalidate로 재사용을 차단한다.|

## 5. 설계 규칙 요약
- 도메인 규칙 위치: 계정 상태/역할, 관리자 session/잠금, 이벤트 노출/예매 가능성, 이미지 3회 재시도/fallback, 좌석 중복 판매, 결제 대기 1개, 결제/환불/DLQ 상태 전이, 감사/운영 로그 보존은 aggregate root 또는 domain service에 둔다.
- 상태 변경 경로: application service는 repository 조회, port 호출, aggregate root 메서드 호출, 저장, 결과 조립만 조정한다. 하위 엔티티/컬렉션 직접 수정과 setter 방식 상태 변경은 금지한다.
- 외부 협력 처리: OAuth, token 발급, 관리자 session 저장소, Toss Payments, 파일 저장소, view count 증가, RabbitMQ/DLQ, 감사/운영 로그, 원문 캡처, metrics 수집은 port로 격리한다.
- BC 경계 보호: 다른 BC의 내부 모델을 직접 수정하지 않고 식별자, snapshot, 판단 결과, command/event만 교환한다. Event BC는 이벤트 탐색/운영/이미지를 함께 소유하고, Booking & Commerce BC는 예매/결제/환불/DLQ 재처리를 하나의 상거래 흐름으로 소유한다.
- 운영/보안 정책: 모든 API p95 1초 미만 및 API별 100 TPS를 관찰하고, 사용자 노출 오류는 사용자 오류/서버 오류로 제한한다. 비밀번호만 암호화하며, 인증/인가 보호 API 감사 로그와 운영 로그는 1개월 보존한다.

## 6. 확인 필요
- 없음
