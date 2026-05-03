# Android Sprint 1.1 Design

문서 상태: v0.2
목표: Android 로컬 차단 프로토타입을 실기기에서 검증 가능한 상태로 보강
현재 상태: 구현 완료. dogfood TSV 분석기는 `pnpm android:dogfood:analyze`로 실행한다.

## 1. Scope

이번 스프린트는 GitHub/API/iOS를 건드리지 않는다. 목표는 Android에서 다음 루프를 실제 기기에서 확인하는 것이다.

```text
권한 허용 -> 차단 대상 저장 -> monitor 시작 -> 대상 앱 foreground 감지 -> credit 0이면 overlay -> credit 있으면 허용
```

Out of scope:

- AccessibilityService
- Device Admin / Device Owner
- 앱 삭제 방지
- 전체 기기 잠금
- 설치 앱 전체 목록 조회
- 서버 sync

## 2. Required Behavior

### Permissions

UI는 세 권한 상태를 항상 표시한다.

| 권한 | granted 기준 | missing일 때 동작 |
| --- | --- | --- |
| Usage Access | `PACKAGE_USAGE_STATS` 접근 가능 | foreground 감지 중단, 설정 버튼 표시 |
| Overlay | `Settings.canDrawOverlays` true | overlay 표시 중단, 설정 버튼 표시 |
| Notification | Android 13+에서 notification permission granted | monitor는 시도하되 UI에 missing 표시 |

Usage Access와 Overlay가 둘 중 하나라도 없으면 monitor service는 overlay를 띄우지 않는다.

### Foreground Detection

- `ForegroundAppReader.currentForegroundPackage()` 결과를 MainActivity에 표시한다.
- 감지 실패 시 `unknown`으로 표시한다.
- 앱 자신의 package는 target과 일치해도 차단하지 않는다.
- deprecated event warning은 Sprint 1.1에서는 허용한다. Android 14/15 대응은 hardening 때 처리한다.

### Credit Controls

UI에 네 개의 테스트 조작을 둔다.

- `Add 5 test minutes`
- `Spend 1 test minute`
- `Reset credit to 0`
- `Save blocked packages`

`remainingMinutes`는 음수가 될 수 없다.

### Strict Mode

Strict mode는 이번 스프린트에서 다음 한 가지 의미만 가진다.

- `strictMode == true`: overlay 내부의 `Add 5 test minutes` 버튼을 숨기거나 비활성화한다.

Strict mode는 삭제 방지, 설정 변경 방지, 권한 회수 방지를 뜻하지 않는다.

### Overlay

overlay는 full-screen으로 표시한다.

필수 표시:

- `Blocked`
- 차단된 package name
- `mock credit is 0 minutes`
- `Open Commit Unlock`

조건부 표시:

- `strictMode == false`이면 `Add 5 test minutes`
- `strictMode == true`이면 test credit shortcut 없음

overlay는 `remainingMinutes > 0`, target mismatch, 권한 missing, service stop 중 하나가 되면 숨긴다.

## 3. Dogfood Event UX

MainActivity에는 별도 debug log가 아니라 단일 dogfood event log를 표시한다. 같은 이벤트 저장소가 in-app log, 14일 summary, TSV export의 source of truth다.

필수 event:

- `permission_missing`
- `monitor_started`
- `monitor_stopped`
- `foreground_changed`
- `target_matched`
- `blocked_attempt`
- `overlay_shown`
- `overlay_hidden`
- `credit_added`
- `credit_spent`
- `credit_auto_spent`
- `credit_reset`

저장 방식:

- SharedPreferences에 최근 1,000개 event를 저장한다.
- 앱 화면에는 최근 50개를 최신순으로 표시한다.
- TSV export는 오래된 순으로 정렬하고 `timestamp`, `type`, `target`, `policy_reason`, `credit_remaining`, `detail` 컬럼을 사용한다.
- TSV analyzer는 export 파일을 읽어 core metrics, Data Quality, Gate A/B/C snapshot, policy reason, target package, daily summary, recommendation을 출력한다.
- 이벤트는 개발 검증용이며 서버로 전송하지 않는다.

## 4. Implementation Notes

권장 변경:

- `CreditStore`에 `resetCredit()` 추가.
- `BlockOverlay.show(...)`에 `strictMode` 또는 `canAddCredit` 인자를 추가.
- `MonitorService`에서 overlay 표시 판단 시 state.strictMode를 넘긴다.
- `DogfoodEventStore`를 MainActivity와 MonitorService가 함께 쓴다.
- `MainActivity`는 `onResume`에서 permission/state/log를 다시 렌더링한다.
- foreground package 표시가 필요하면 MainActivity에서도 `ForegroundAppReader`를 읽는다.

피해야 할 변경:

- Compose 도입
- 앱 목록 조회를 위한 `QUERY_ALL_PACKAGES` 추가
- AccessibilityService 추가
- foreground polling interval을 1초보다 더 공격적으로 줄이기

## 5. Acceptance Tests

Repo:

- `./gradlew :apps:android:assembleDebug`
- `./gradlew :apps:android:lintDebug`
- `pnpm android:dogfood:analyze <sample.tsv>`
- `pnpm test`
- `pnpm build`
- `pnpm typecheck`

Device:

1. `adb devices -l`에 기기가 표시된다.
2. APK 설치 후 앱을 열면 Usage Access와 Overlay가 missing으로 표시된다.
3. 두 권한을 허용하면 granted로 바뀐다.
4. blocked target에 `com.android.chrome`을 저장한다.
5. credit을 0으로 reset한다.
6. monitor를 start한다.
7. Chrome을 foreground로 열면 overlay가 표시된다.
8. Commit Unlock으로 돌아와 credit을 5로 만든다.
9. Chrome을 다시 열면 overlay가 표시되지 않는다.
10. strictMode true, credit 0에서 overlay의 test credit shortcut이 보이지 않는다.
11. 앱 재시작 후 credit state와 blocked targets가 유지된다.

## 6. Failure Handling

| 실패 | 처리 |
| --- | --- |
| Usage Access가 이벤트를 반환하지 않음 | UI에 `unknown` 표시, dogfood event 기록 |
| Overlay 권한이 회수됨 | overlay 숨김, permission missing 표시 |
| Foreground service가 OS에 의해 중지됨 | 사용자가 앱으로 돌아오면 monitor stopped로 표시 |
| target package를 잘못 입력함 | overlay가 안 뜨는 것이 정상. 최근 foreground package 표시로 사용자가 수정 |
| Android 13 notification denied | monitor 가능 여부와 별개로 permission warning 표시 |
