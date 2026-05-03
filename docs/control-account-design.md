# Control And Account Design

문서 상태: v0.1
작성일: 2026-05-04
역할: 기기별 차단 가능 범위, 사용자 선택 대상, 앱 삭제 가능성, 로그인/회원가입/로그아웃/회원탈퇴 UX 기준

상위 기획/보안/개인정보 gate는 [product-security-hardening-plan.md](product-security-hardening-plan.md)를 따른다.

## 1. Product Decision

Commit-to-Unlock은 사용자의 모든 서비스를 막는 앱이 아니다. 사용자가 직접 고른 방해 앱/웹사이트만 정책 대상이 된다.

제품 원칙:

- 사용자가 선택하지 않은 앱/웹사이트는 막지 않는다.
- 앱 자기 자신, OS 설정, 계정/탈퇴/삭제/권한 화면은 막지 않는다.
- 앱은 삭제 가능해야 한다. 삭제 방지, Device Admin, MDM 없는 uninstall prevention은 금지한다.
- 앱이 설치되어 있고 권한이 켜져 있을 때만 선택한 target에 대해 차단을 시도한다.
- 차단은 “감옥”이 아니라 “사용자가 선택한 개발자용 마찰”이다.
- 계정 생성이 들어가면 로그인, 로그아웃, 회원탈퇴, 데이터 삭제, GitHub 연결 해제를 모두 설계한다.

핵심 UX 문장:

```text
당신이 고른 방해 앱만 막습니다. 앱은 언제든 삭제할 수 있습니다. 삭제하지 않고 켜두는 동안에는, 당신이 정한 규칙대로 작동합니다.
```

금지 문구:

- `삭제할 수 없는 앱`
- `휴대폰 전체 잠금`
- `모든 서비스 차단`
- `AI가 강제로 통제`
- `설정을 못 바꾸게 막음`

허용 문구:

- `선택한 앱만 차단`
- `언제든 권한을 끄거나 앱을 삭제할 수 있음`
- `앱을 계속 켜두기로 선택한 경우, 정책을 지킴`
- `로그아웃과 회원탈퇴는 차단 대상이 아님`

## 2. Device Enforcement Matrix

| 플랫폼 | 실제 가능한 통제 | 불가능/금지 | MVP 약속 |
| --- | --- | --- | --- |
| Android B2C | Usage Access로 foreground app 감지, overlay로 차단 화면 표시, foreground service로 감시 | 전체 폰 잠금, 삭제 방지, Accessibility 자동조작, 모든 앱 스캔 | selected package blocking prototype |
| Android managed device | Device Policy Controller/MDM이면 더 강한 kiosk/lock-task 가능 | 개인 B2C 앱에서 managed-device 기능을 암시하면 안 됨 | MVP 범위 밖 |
| iOS B2C | FamilyControls/ManagedSettings로 사용자가 선택한 앱/도메인 shield | 임의 전체 기기 잠금, 앱 이름 직접 노출 약속, entitlement 없이 배포 | selected app/domain shield |
| iOS supervised/MDM | 기관 소유 supervised 기기에서 더 강한 restriction 가능 | 개인용 앱과 같은 제품으로 섞지 않음 | 학교/기관 버전까지 보류 |
| Desktop/browser future | browser extension, desktop helper, DNS/VPN 등 후보 | 모바일 MVP 성공 전 확장 금지 | fallback spike only |

기기별 QA 기준:

- Android는 제조사별 overlay/background 제한이 다르므로 device matrix를 기록한다.
- iOS는 entitlement와 실제 device shield 동작을 별도 gate로 둔다.
- 플랫폼별로 “무엇을 못 하는지”를 onboarding에 명확히 보여준다.

## 3. Target Selection Design

### 3.1 Default State

기본값은 아무것도 차단하지 않는다.

첫 설정 흐름:

```text
권한 설명
-> target 선택
-> 요일/시간 선택
-> mock credit / proof 설명
-> smoke test
-> 정책 활성화
```

사용자가 target을 선택하지 않으면:

- monitor를 켤 수는 있지만 차단은 일어나지 않는다.
- UI는 `차단 대상 없음`을 명확히 보여준다.
- 앱이 자동으로 SNS/YouTube/브라우저를 추가하지 않는다.

### 3.2 Android Target Rules

Android MVP target은 package name이다.

허용:

- 사용자가 직접 입력한 package
- Usage Access 승인 후 최근 foreground package 중 사용자가 고른 package

금지:

- `QUERY_ALL_PACKAGES` 기반 전체 설치 앱 스캔
- 기본으로 모든 브라우저/SNS 자동 차단
- 앱 자기 자신의 package 추가
- OS 설정, 전화/긴급전화, launcher, accessibility/settings 계열 system package 차단

필수 guard:

- target 저장 시 own package는 제거한다.
- target 저장 시 빈 값, 중복 값, 공백을 제거한다.
- future production에서는 system-critical package denylist를 둔다.

### 3.3 iOS Target Rules

iOS는 FamilyActivityPicker에서 사용자가 직접 선택한다.

표시 원칙:

- 앱 내부에서 iOS app token을 사람이 읽을 수 있는 이름처럼 표현하지 않는다.
- `선택한 앱 3개`, `선택한 웹 도메인 2개`처럼 개수 중심으로 표시한다.
- shield는 mock/server credit 상태에 따라 apply/release 한다.

## 4. Blocking UX

차단 화면은 사용자를 혼내지 않는다. 대신 다음 세 가지를 보여준다.

1. 왜 막혔는가
2. 어떤 대상이 막혔는가
3. 어떻게 풀 수 있는가

Android overlay 필수 정보:

| 항목 | 예 |
| --- | --- |
| Target | `com.google.android.youtube` |
| Reason | `credit_empty` |
| Remaining credit | `0 minutes` |
| Next action | `작은 PR, mock proof, emergency unlock 중 하나를 선택하세요.` |
| Exit | `Commit-to-Unlock으로 돌아가기` |

차단 화면에서 금지:

- 계정 삭제/로그아웃/권한 화면을 막는 링크
- 비난성 카피
- 앱 삭제를 어렵게 만드는 설명
- 모든 앱이 차단된 것처럼 보이는 문구

## 5. App Deletion And Tamper Boundaries

앱 삭제 가능성은 제품 약속에 포함한다.

정책:

- 앱 삭제 방지는 하지 않는다.
- Android Device Admin/Device Owner는 B2C MVP에서 사용하지 않는다.
- iOS supervised/MDM 삭제 방지는 학교/기관 버전 전까지 언급하지 않는다.
- 권한을 끄거나 앱을 삭제하면 차단은 멈춘다.
- 앱이 설치되어 있고 권한이 켜져 있으면, 선택 target에 대해 정책을 실행한다.

사용자-facing 설명:

```text
이 앱은 당신의 기기를 빼앗지 않습니다. 앱 삭제와 권한 해제는 언제든 가능합니다. 다만 앱을 켜두기로 선택했다면, 선택한 방해 앱은 설정한 규칙에 따라 막습니다.
```

Dogfood에서 관찰할 신호:

- permission_missing 빈도
- monitor_stopped 빈도
- 앱 삭제/재설치
- emergency unlock 빈도
- manual credit add 빈도

이 신호가 높으면 강도를 높이는 것이 아니라 UX/정책을 낮춘다.

## 6. Account Model

### 6.1 Local MVP

현재 Android MVP는 계정이 없다.

로컬 MVP에서 제공:

- local credit state
- local target list
- local dogfood log
- local export
- local clear

로컬 MVP에서 제공하지 않음:

- 회원가입
- 로그인
- 로그아웃
- 회원탈퇴
- GitHub OAuth/App 연결
- 서버 sync

### 6.2 Server MVP

Sprint 4 이후 서버 sync를 열면 계정 모델이 필요하다.

권장:

- email magic link 또는 passkey 우선
- GitHub App 설치는 proof source 연결이지, 유일한 로그인 수단으로 고정하지 않는다.
- GitHub 연결 해제와 계정 탈퇴를 분리한다.

계정 상태:

| 상태 | 의미 |
| --- | --- |
| anonymous_local | local-only 사용 |
| signed_out | 서버 sync 없음. local controls는 접근 가능 |
| signed_in | 서버 ledger sync 가능 |
| github_connected | GitHub installation/repo proof 사용 가능 |
| deletion_requested | 탈퇴 요청 접수, 서버 data deletion 진행 |
| deleted | 서버 계정 삭제 완료 |

## 7. Auth UX Requirements

### Sign Up

회원가입은 GitHub proof ledger가 실제로 필요해지는 순간에만 요청한다.

권장 흐름:

```text
local dogfood 사용
-> GitHub proof ledger 사용 선택
-> account 생성
-> GitHub App 설치
-> repo allowlist 선택
```

회원가입 전에 설명할 것:

- 어떤 데이터가 서버로 올라가는지
- GitHub raw diff를 기본 저장하지 않는지
- 계정 삭제 시 무엇이 삭제되는지
- 앱을 삭제해도 서버 계정은 자동 삭제되지 않는지

### Login

로그인은 sync와 ledger 조회를 위한 기능이다.

로그인 실패 시:

- local controls 화면은 계속 접근 가능해야 한다.
- 차단 설정을 바꾸거나 끌 수 있어야 한다.
- 계정 삭제/도움말 링크는 접근 가능해야 한다.

### Logout

로그아웃은 trapping UX가 되면 안 된다.

로그아웃 시:

- server sync 중단
- server ledger cache clear
- local target/policy는 사용자 선택에 따라 유지 또는 비활성화
- account deletion, GitHub disconnect, privacy export 링크는 계속 표시

권장 옵션:

```text
로그아웃
[ ] 이 기기의 local blocking도 끄기
```

기본값은 local blocking 유지가 아니라 사용자 확인이다. 사용자가 뭘 선택했는지 명확해야 한다.

### Delete Account

회원탈퇴는 앱 안에서 찾기 쉬워야 하고, 웹에서도 요청 가능해야 한다.

삭제 대상:

- account profile
- GitHub installation mapping
- repository allowlist
- proof events
- feature vectors
- score decisions
- server credit ledger, unless user explicitly keeps exported/local history
- mobile sync tokens

삭제 후:

- GitHub enrichment 중단
- mobile server sync 중단
- local app은 signed-out/local-only 상태가 된다
- 사용자는 앱 삭제 또는 local data clear를 선택할 수 있다

탈퇴는 `비활성화`로 표현하지 않는다. 보류 기간이 있더라도 최종 삭제 일자를 명확히 표시한다.

## 8. Settings IA

최소 settings 구조:

```text
Settings
  Account
    Sign up / Login
    Logout
    Delete account
  GitHub
    Connect GitHub
    Connected repositories
    Disconnect GitHub
    Delete GitHub-derived data
  Blocking
    Blocked targets
    Schedule
    Strict mode
    Emergency unlock
  Privacy
    Local data export
    Clear local dogfood events
    Server data export
    Data retention
  Device
    Usage Access status
    Overlay status
    Notification status
    Device capability smoke result
```

Settings는 절대 차단 대상이 아니다.

## 9. Design Quality Bar

이 제품의 디자인은 "귀여운 잠금 앱"보다 개발자 도구에 가깝다.

기준:

- Home은 ledger/status 중심으로 dense하게 구성한다.
- 계정/삭제/권한 화면은 장난 없이 정확하게 쓴다.
- target selection은 실수 방지가 우선이다.
- 사용자가 차단 대상과 아닌 대상을 한눈에 구분해야 한다.
- 버튼에는 위험도를 반영한다: normal, warning, destructive.
- destructive action은 `Delete account`, `Clear local data`, `Disconnect GitHub`로 명확히 분리한다.

화면별 톤:

| 화면 | 톤 |
| --- | --- |
| Developer Gate | playful |
| Blocking Overlay | playful but actionable |
| Home/Ledger | developer utility |
| Permissions | precise |
| Account/Delete | formal and clear |
| Privacy | formal and clear |

## 10. Platform Policy Notes

2026-05-04 확인 기준:

- Apple은 계정 생성을 지원하는 앱에 계정 삭제 제공을 요구한다. Sign in with Apple을 쓰면 token revoke도 처리해야 한다.
- Google Play는 앱 안에서 계정을 만들 수 있으면 앱 안과 앱 밖에서 계정 삭제 요청 경로를 제공해야 하고, 계정 삭제 시 관련 사용자 데이터도 삭제해야 한다.
- Apple FamilyControls/ManagedSettings는 사용자가 선택한 앱/웹 도메인 shield에 맞는 모델이다.
- Android Usage Access/Overlay는 B2C self-control prototype에 맞지만, AccessibilityService/Device Admin/전체 잠금 약속은 MVP 범위 밖으로 유지한다.

Sources:

- Apple: [Offering account deletion in your app](https://developer.apple.com/support/offering-account-deletion-in-your-app/)
- Apple: [FamilyControls](https://developer.apple.com/documentation/familycontrols)
- Apple: [ManagedSettings shield](https://developer.apple.com/documentation/managedsettings/managedsettingsstore/shield)
- Google Play: [User Data policy](https://support.google.com/googleplay/android-developer/answer/10144311)
- Google Play: [Account deletion requirements](https://support.google.com/googleplay/android-developer/answer/13327111)
