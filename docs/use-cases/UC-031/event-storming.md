# UC-031. `Notification Recipient`가 자신의 `Notification`을 삭제한다 이벤트 스토밍

## 1. 시작 유스케이스
- 유스케이스: `UC-031. Notification Recipient deletes their Notifications`
- 액터: `Notification Recipient`
- 목표: 자신의 `Recipient User ID`로 수신된 `Notification Inbox`의 `Notification`을 단건, `selected-set`, `all-owned` 범위로 삭제한다.
- 초기 커맨드: 🟦 단건 `Notification` 삭제를 요청하라

### 사전 조건
- 요청자는 인증되어 있다.
- 요청자는 단건, `selected-set`, `all-owned` 중 하나의 삭제 범위를 선택한다.

### 종료 조건
- 성공: 승인된 범위 안의 요청자 소유 `Notification`만 삭제되고 삭제 결과가 반환된다.
- 실패: 비인증 삭제 요청이 거절되거나, 요청 범위에 다른 수신자 소유 `Notification`이 포함되면 삭제가 거절된다.

---
## 2. 흐름
### [Flow: 단건 `Notification` 삭제]
🟦 단건 `Notification` 삭제를 요청하라
→ 🟧 단건 `Notification` 삭제가 요청되었다
→ 🟪 요청자가 인증된 경우
→ 🟦 단건 삭제 대상을 식별하라
→ 🟧 단건 삭제 대상이 식별되었다
→ 🟦 단건 삭제 대상 `Notification`의 소유권을 확인하라
→ 🟧 단건 삭제 대상 `Notification`의 소유권이 확인되었다
→ 🟪 단건 삭제 대상 `Notification`이 요청자에게 속하는 경우
→ 🟦 단건 삭제 대상 `Notification`을 삭제하라
→ 🟧 단건 삭제 대상 `Notification`이 삭제되었다
→ 🟦 단건 삭제 결과를 반환하라
→ 🟧 단건 삭제 결과가 반환되었다

---
### [Flow: `selected-set` `Notification` 삭제]
🟦 `selected-set` `Notification` 삭제를 요청하라
→ 🟧 `selected-set` `Notification` 삭제가 요청되었다
→ 🟪 요청자가 인증된 경우
→ 🟦 `selected-set` 삭제 대상을 식별하라
→ 🟧 `selected-set` 삭제 대상이 식별되었다
→ 🟦 `selected-set`의 타인 소유 포함 여부를 확인하라
→ 🟧 `selected-set`의 타인 소유 포함 여부가 확인되었다
→ 🟪 `selected-set`에 타인 소유 `Notification`이 포함되지 않은 경우
→ 🟦 기존 owned `Notification`만 삭제 대상으로 추려라
→ 🟧 기존 owned `Notification` 삭제 대상이 추려졌다
→ 🟪 삭제할 기존 owned `Notification`이 남아 있는 경우
→ 🟦 남아 있는 기존 owned `Notification`을 삭제하라
→ 🟧 남아 있는 기존 owned `Notification`이 삭제되었다
→ 🟦 `selected-set` 삭제 결과를 반환하라
→ 🟧 `selected-set` 삭제 결과가 반환되었다

---
### [Flow: `selected-set` 삭제에서 이미 없는 owned `Notification` 무시]
🟦 `selected-set` `Notification` 삭제를 요청하라
→ 🟧 `selected-set` `Notification` 삭제가 요청되었다
→ 🟪 요청자가 인증된 경우
→ 🟦 `selected-set` 삭제 대상을 식별하라
→ 🟧 `selected-set` 삭제 대상이 식별되었다
→ 🟦 `selected-set`의 타인 소유 포함 여부를 확인하라
→ 🟧 `selected-set`의 타인 소유 포함 여부가 확인되었다
→ 🟪 `selected-set`에 타인 소유 `Notification`이 포함되지 않은 경우
→ 🟦 기존 owned `Notification`만 삭제 대상으로 추려라
→ 🟧 기존 owned `Notification` 삭제 대상이 추려졌다
→ 🟪 이미 없는 owned `Notification`이 포함된 경우
→ 🟧 이미 없는 owned `Notification`이 삭제 대상에서 제외되었다
→ 🟪 삭제할 기존 owned `Notification`이 남아 있는 경우
→ 🟦 남아 있는 기존 owned `Notification`을 삭제하라
→ 🟧 남아 있는 기존 owned `Notification`이 삭제되었다
→ 🟦 `selected-set` 삭제 결과를 반환하라
→ 🟧 `selected-set` 삭제 결과가 반환되었다

---
### [Flow: `selected-set` 삭제에서 모든 owned `Notification`이 이미 없는 경우]
🟦 `selected-set` `Notification` 삭제를 요청하라
→ 🟧 `selected-set` `Notification` 삭제가 요청되었다
→ 🟪 요청자가 인증된 경우
→ 🟦 `selected-set` 삭제 대상을 식별하라
→ 🟧 `selected-set` 삭제 대상이 식별되었다
→ 🟦 `selected-set`의 타인 소유 포함 여부를 확인하라
→ 🟧 `selected-set`의 타인 소유 포함 여부가 확인되었다
→ 🟪 `selected-set`에 타인 소유 `Notification`이 포함되지 않은 경우
→ 🟦 기존 owned `Notification`만 삭제 대상으로 추려라
→ 🟧 기존 owned `Notification` 삭제 대상이 추려졌다
→ 🟪 삭제할 기존 owned `Notification`이 남아 있지 않은 경우
→ 🟦 `selected-set` 삭제 결과를 반환하라
→ 🟧 `selected-set` 삭제 결과가 반환되었다

---
### [Flow: `all-owned` `Notification` 삭제]
🟦 `all-owned` `Notification` 삭제를 요청하라
→ 🟧 `all-owned` `Notification` 삭제가 요청되었다
→ 🟪 요청자가 인증된 경우
→ 🟦 요청자 소유 전체 `Notification` 삭제 범위를 확정하라
→ 🟧 요청자 소유 전체 `Notification` 삭제 범위가 확정되었다
→ 🟦 요청자 소유 전체 `Notification`을 삭제하라
→ 🟧 요청자 소유 전체 `Notification`이 삭제되었다
→ 🟦 `all-owned` 삭제 결과를 반환하라
→ 🟧 `all-owned` 삭제 결과가 반환되었다

---
### [Flow: 비인증 삭제 요청 거절]
🟦 단건 `Notification` 삭제를 요청하라
→ 🟧 단건 `Notification` 삭제가 요청되었다
→ 🟪 요청자가 인증되지 않은 경우
→ 🟧 단건 `Notification` 삭제 요청이 인증 부족으로 거절되었다

🟦 `selected-set` `Notification` 삭제를 요청하라
→ 🟧 `selected-set` `Notification` 삭제가 요청되었다
→ 🟪 요청자가 인증되지 않은 경우
→ 🟧 `selected-set` `Notification` 삭제 요청이 인증 부족으로 거절되었다

🟦 `all-owned` `Notification` 삭제를 요청하라
→ 🟧 `all-owned` `Notification` 삭제가 요청되었다
→ 🟪 요청자가 인증되지 않은 경우
→ 🟧 `all-owned` `Notification` 삭제 요청이 인증 부족으로 거절되었다

---
### [Flow: 타인 소유 `Notification` 포함 삭제 요청 거절]
🟦 단건 `Notification` 삭제를 요청하라
→ 🟧 단건 `Notification` 삭제가 요청되었다
→ 🟪 요청자가 인증된 경우
→ 🟦 단건 삭제 대상을 식별하라
→ 🟧 단건 삭제 대상이 식별되었다
→ 🟦 단건 삭제 대상 `Notification`의 소유권을 확인하라
→ 🟧 단건 삭제 대상 `Notification`의 소유권이 확인되었다
→ 🟪 단건 삭제 대상 `Notification`이 요청자에게 속하지 않는 경우
→ 🟧 단건 `Notification` 삭제가 소유권 불일치로 거절되었다

🟦 `selected-set` `Notification` 삭제를 요청하라
→ 🟧 `selected-set` `Notification` 삭제가 요청되었다
→ 🟪 요청자가 인증된 경우
→ 🟦 `selected-set` 삭제 대상을 식별하라
→ 🟧 `selected-set` 삭제 대상이 식별되었다
→ 🟦 `selected-set`의 타인 소유 포함 여부를 확인하라
→ 🟧 `selected-set`의 타인 소유 포함 여부가 확인되었다
→ 🟪 `selected-set`에 타인 소유 `Notification`이 포함된 경우
→ 🟧 `selected-set` `Notification` 삭제가 소유권 불일치로 거절되었다

---
## 3. 도메인 요소 (통합)
|유형|내용|트리거|결과|시스템|비고|
|---|---|---|---|---|---|
|🟦|단건 `Notification` 삭제를 요청하라|`Notification Recipient`|단건 `Notification` 삭제가 요청되었다|`Gateway 인증 계층`|단건 삭제 시작 커맨드|
|🟦|단건 삭제 대상을 식별하라|요청자가 인증된 경우|단건 삭제 대상이 식별되었다|`Notification 삭제 시스템`|요청된 `Notification ID` 기준|
|🟦|단건 삭제 대상 `Notification`의 소유권을 확인하라|단건 삭제 대상이 식별되었다|단건 삭제 대상 `Notification`의 소유권이 확인되었다|`Notification 삭제 시스템`|요청자 소유 여부 확인|
|🟦|단건 삭제 대상 `Notification`을 삭제하라|단건 삭제 대상 `Notification`이 요청자에게 속하는 경우|단건 삭제 대상 `Notification`이 삭제되었다|`Notification 삭제 시스템`|요청자 소유 단건만 삭제|
|🟦|단건 삭제 결과를 반환하라|단건 삭제 대상 `Notification`이 삭제되었다|단건 삭제 결과가 반환되었다|`Notification 삭제 시스템`|정상 단건 삭제 응답|
|🟦|`selected-set` `Notification` 삭제를 요청하라|`Notification Recipient`|`selected-set` `Notification` 삭제가 요청되었다|`Gateway 인증 계층`|선택 삭제 시작 커맨드|
|🟦|`selected-set` 삭제 대상을 식별하라|요청자가 인증된 경우|`selected-set` 삭제 대상이 식별되었다|`Notification 삭제 시스템`|요청된 `Notification ID` 집합 기준|
|🟦|`selected-set`의 타인 소유 포함 여부를 확인하라|`selected-set` 삭제 대상이 식별되었다|`selected-set`의 타인 소유 포함 여부가 확인되었다|`Notification 삭제 시스템`|타인 소유 포함 시 전체 거절 판단|
|🟦|기존 owned `Notification`만 삭제 대상으로 추려라|`selected-set`에 타인 소유 `Notification`이 포함되지 않은 경우|기존 owned `Notification` 삭제 대상이 추려졌다|`Notification 삭제 시스템`|이미 없는 owned `Notification` 제외 단계|
|🟦|남아 있는 기존 owned `Notification`을 삭제하라|삭제할 기존 owned `Notification`이 남아 있는 경우|남아 있는 기존 owned `Notification`이 삭제되었다|`Notification 삭제 시스템`|남아 있는 owned 항목만 삭제|
|🟦|`selected-set` 삭제 결과를 반환하라|기존 owned `Notification` 삭제 대상이 추려졌거나 남아 있는 기존 owned `Notification`이 삭제되었다|`selected-set` 삭제 결과가 반환되었다|`Notification 삭제 시스템`|부분 삭제 또는 0건 삭제 포함 결과 반환|
|🟦|`all-owned` `Notification` 삭제를 요청하라|`Notification Recipient`|`all-owned` `Notification` 삭제가 요청되었다|`Gateway 인증 계층`|전체 owned 삭제 시작 커맨드|
|🟦|요청자 소유 전체 `Notification` 삭제 범위를 확정하라|요청자가 인증된 경우|요청자 소유 전체 `Notification` 삭제 범위가 확정되었다|`Notification 삭제 시스템`|요청자 `Recipient User ID` 범위 전체|
|🟦|요청자 소유 전체 `Notification`을 삭제하라|요청자 소유 전체 `Notification` 삭제 범위가 확정되었다|요청자 소유 전체 `Notification`이 삭제되었다|`Notification 삭제 시스템`|요청자 owned 전건 삭제|
|🟦|`all-owned` 삭제 결과를 반환하라|요청자 소유 전체 `Notification`이 삭제되었다|`all-owned` 삭제 결과가 반환되었다|`Notification 삭제 시스템`|정상 전체 삭제 응답|
|🟧|단건 `Notification` 삭제가 요청되었다|단건 `Notification` 삭제를 요청하라|인증 정책 평가|`Gateway 인증 계층`|단건 삭제 진입|
|🟧|단건 삭제 대상이 식별되었다|단건 삭제 대상을 식별하라|소유권 확인|`Notification 삭제 시스템`|단건 대상 식별 완료|
|🟧|단건 삭제 대상 `Notification`의 소유권이 확인되었다|단건 삭제 대상 `Notification`의 소유권을 확인하라|소유/비소유 정책 평가|`Notification 삭제 시스템`|단건 삭제 허용 여부 판단|
|🟧|단건 삭제 대상 `Notification`이 삭제되었다|단건 삭제 대상 `Notification`을 삭제하라|단건 삭제 결과 반환|`Notification 삭제 시스템`|단건 삭제 완료|
|🟧|단건 삭제 결과가 반환되었다|단건 삭제 결과를 반환하라|단건 삭제 흐름 종료|`Notification 삭제 시스템`|단건 결과 응답|
|🟧|`selected-set` `Notification` 삭제가 요청되었다|`selected-set` `Notification` 삭제를 요청하라|인증 정책 평가|`Gateway 인증 계층`|선택 삭제 진입|
|🟧|`selected-set` 삭제 대상이 식별되었다|`selected-set` 삭제 대상을 식별하라|타인 소유 포함 여부 확인|`Notification 삭제 시스템`|선택 삭제 대상 식별 완료|
|🟧|`selected-set`의 타인 소유 포함 여부가 확인되었다|`selected-set`의 타인 소유 포함 여부를 확인하라|거절 또는 owned 후보 추리기|`Notification 삭제 시스템`|타인 소유 포함 여부만 판단|
|🟧|기존 owned `Notification` 삭제 대상이 추려졌다|기존 owned `Notification`만 삭제 대상으로 추려라|이미 없는 owned 제외 정책 또는 삭제 실행 정책 평가|`Notification 삭제 시스템`|이미 없는 owned 제외 후 남은 대상 집합|
|🟧|이미 없는 owned `Notification`이 삭제 대상에서 제외되었다|이미 없는 owned `Notification`이 포함된 경우|남은 기존 owned 삭제 또는 0건 결과 반환|`Notification 삭제 시스템`|선택 삭제 예외 허용 이벤트|
|🟧|남아 있는 기존 owned `Notification`이 삭제되었다|남아 있는 기존 owned `Notification`을 삭제하라|선택 삭제 결과 반환|`Notification 삭제 시스템`|남은 owned만 삭제 완료|
|🟧|`selected-set` 삭제 결과가 반환되었다|`selected-set` 삭제 결과를 반환하라|`selected-set` 삭제 흐름 종료|`Notification 삭제 시스템`|삭제된 owned 수와 무시된 이미 없는 owned 상태를 반영한 결과|
|🟧|`all-owned` `Notification` 삭제가 요청되었다|`all-owned` `Notification` 삭제를 요청하라|인증 정책 평가|`Gateway 인증 계층`|전체 owned 삭제 진입|
|🟧|요청자 소유 전체 `Notification` 삭제 범위가 확정되었다|요청자 소유 전체 `Notification` 삭제 범위를 확정하라|전체 owned 삭제 실행|`Notification 삭제 시스템`|요청자 범위 전건 결정|
|🟧|요청자 소유 전체 `Notification`이 삭제되었다|요청자 소유 전체 `Notification`을 삭제하라|전체 삭제 결과 반환|`Notification 삭제 시스템`|전체 owned 삭제 완료|
|🟧|`all-owned` 삭제 결과가 반환되었다|`all-owned` 삭제 결과를 반환하라|`all-owned` 삭제 흐름 종료|`Notification 삭제 시스템`|전체 owned 삭제 응답|
|🟧|단건 `Notification` 삭제 요청이 인증 부족으로 거절되었다|요청자가 인증되지 않은 경우|삭제 거절 응답 반환|`Gateway 인증 계층`|비인증 단건 삭제 차단|
|🟧|`selected-set` `Notification` 삭제 요청이 인증 부족으로 거절되었다|요청자가 인증되지 않은 경우|삭제 거절 응답 반환|`Gateway 인증 계층`|비인증 선택 삭제 차단|
|🟧|`all-owned` `Notification` 삭제 요청이 인증 부족으로 거절되었다|요청자가 인증되지 않은 경우|삭제 거절 응답 반환|`Gateway 인증 계층`|비인증 전체 삭제 차단|
|🟧|단건 `Notification` 삭제가 소유권 불일치로 거절되었다|단건 삭제 대상 `Notification`이 요청자에게 속하지 않는 경우|삭제 실패 응답 반환|`Notification 삭제 시스템`|타인 소유 단건 삭제 차단|
|🟧|`selected-set` `Notification` 삭제가 소유권 불일치로 거절되었다|`selected-set`에 타인 소유 `Notification`이 포함된 경우|삭제 실패 응답 반환|`Notification 삭제 시스템`|타인 소유 포함 시 선택 삭제 전체 거절|
|🟪|요청자가 인증된 경우|각 삭제 요청 이벤트|삭제 대상 확인 또는 삭제 범위 확정|`Gateway 인증 계층`|공통 인증 통과|
|🟪|요청자가 인증되지 않은 경우|각 삭제 요청 이벤트|인증 부족 거절 이벤트|`Gateway 인증 계층`|공통 인증 차단|
|🟪|단건 삭제 대상 `Notification`이 요청자에게 속하는 경우|단건 삭제 대상 `Notification`의 소유권이 확인되었다|단건 삭제 대상 `Notification`을 삭제하라|`Notification 삭제 시스템`|요청자 소유 단건만 삭제 허용|
|🟪|단건 삭제 대상 `Notification`이 요청자에게 속하지 않는 경우|단건 삭제 대상 `Notification`의 소유권이 확인되었다|단건 `Notification` 삭제가 소유권 불일치로 거절되었다|`Notification 삭제 시스템`|타인 소유 단건 삭제 거절|
|🟪|`selected-set`에 타인 소유 `Notification`이 포함되지 않은 경우|`selected-set`의 타인 소유 포함 여부가 확인되었다|기존 owned `Notification`만 삭제 대상으로 추려라|`Notification 삭제 시스템`|선택 삭제 계속 진행|
|🟪|`selected-set`에 타인 소유 `Notification`이 포함된 경우|`selected-set`의 타인 소유 포함 여부가 확인되었다|`selected-set` `Notification` 삭제가 소유권 불일치로 거절되었다|`Notification 삭제 시스템`|타인 소유 포함 즉시 전체 거절|
|🟪|이미 없는 owned `Notification`이 포함된 경우|기존 owned `Notification` 삭제 대상이 추려졌다|이미 없는 owned `Notification`이 삭제 대상에서 제외되었다|`Notification 삭제 시스템`|이미 없는 owned은 실패 사유가 아니라 무시 대상|
|🟪|삭제할 기존 owned `Notification`이 남아 있는 경우|기존 owned `Notification` 삭제 대상이 추려졌거나 이미 없는 owned `Notification`이 삭제 대상에서 제외되었다|남아 있는 기존 owned `Notification`을 삭제하라|`Notification 삭제 시스템`|부분 성공 삭제 허용|
|🟪|삭제할 기존 owned `Notification`이 남아 있지 않은 경우|기존 owned `Notification` 삭제 대상이 추려졌다|`selected-set` 삭제 결과를 반환하라|`Notification 삭제 시스템`|모든 owned이 이미 없어도 요청 전체는 정상 종료|
|🟩|없음|없음|없음|외부|이 유스케이스는 외부 시스템 협력이 필요 없다|

---
## 4. 외부 시스템
|시스템|연동 목적|관련 유스케이스|비고|
|---|---|---|---|
|없음|없음|`UC-031`|내부 `Gateway 인증 계층`과 `Notification 삭제 시스템`만 사용|

---
## 5. 규칙 (Invariant)
- 모든 삭제 결과는 인증된 요청자의 `Recipient User ID` 범위 안의 owned `Notification`에만 적용된다.
- 비인증 삭제 요청은 `Notification 삭제 시스템`으로 전달되기 전에 `Gateway 인증 계층`에서 차단된다.
- 단건 삭제와 `selected-set` 삭제는 요청 범위에 타인 소유 `Notification`이 하나라도 포함되면 거절된다.
- `selected-set` 삭제는 이미 없는 owned `Notification`을 실패 사유로 만들지 않고 삭제 대상에서 제외한 뒤 남아 있는 기존 owned `Notification`만 삭제한다.
- `selected-set` 삭제에서 삭제할 기존 owned `Notification`이 남아 있지 않아도 요청은 정상 종료되고 결과가 반환된다.
- `all-owned` 삭제는 요청 시점의 요청자 owned `Notification` 전체 범위에만 적용된다.

---
## 6. 확인 필요
- 없음
