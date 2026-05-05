# MVP Gap Analysis

문서 상태: v0.2
조사일: 2026-05-04
최종 정리: 2026-05-05
역할: 현재 MVP에서 부족한 점, 최신 플랫폼/경쟁 서비스 조사 반영, 다음 보완 우선순위를 한 곳에 고정한다.

## 1. Executive Verdict

현재 Android MVP는 1차 구현 기준으로 `code-complete`다. 제품 판단은 아직 `evidence-incomplete`다.

이번 점검 결론:

1. Android 로컬 차단 플로우는 에뮬레이터에서 검증됐다.
2. target guardrail, foreground resolver, monitor heartbeat state까지 1차 안정화는 끝났다.
3. 아직 실제 물리 Android 기기 smoke evidence가 없다.
4. 모바일 차단만으로는 유료화가 약하다. paid moat는 `developer proof ledger + desktop/browser companion + sync/history`다.
5. GitHub scoring은 계속 보류한다. 먼저 real-device Gate A/D evidence와 webhook security foundation이 필요하다.
6. 지금 추가 구현할 1순위는 새 기능이 아니라 dogfood evidence와 desktop/browser companion 설계다.

## 2. Current Evidence

| 영역 | 현재 증거 | 판단 |
| --- | --- | --- |
| Android build | Gradle assemble/lint/unit test 통과 | pass |
| Repo checks | `pnpm test`, `pnpm build`, `pnpm typecheck` 통과 | pass |
| Emulator enforcement | Android 13 AVD에서 Chrome 0분 차단, +5분 허용, 60초 후 1분 자동 차감 확인 | pass |
| Monitor runtime state | desired state와 heartbeat-backed runtime state 분리, stale 판정 unit test 추가 | pass |
| Real device enforcement | 실제 물리 기기 연결 검증 없음 | blocker |
| Dogfood duration | 14일 TSV 세트 없음 | blocker |
| Monetization evidence | 경쟁 서비스 기준 mobile-only blocker는 free/low-price anchor가 강함 | weak |
| GitHub proof supply | 실제 사용자의 14일 PR/commit/proof 빈도 없음 | needs_data |

에뮬레이터에서 발견해 수정한 중요한 버그:

- `UsageStatsManager`는 지속적인 "현재 앱" 값을 주는 API가 아니라 최근 activity event를 조회한다.
- overlay 표시 중에는 우리 앱 또는 `null`이 foreground처럼 보일 수 있었다.
- 이 때문에 차단 overlay가 10초 뒤 사라지고, 크레딧 자동 차감도 멈출 수 있었다.
- `ForegroundPackageResolver`를 추가해 overlay/최근 foreground 이벤트 공백을 안정화했다.

남은 실제 기기 검증 이유:

- 제조사별 배터리/백그라운드 제한이 다르다.
- overlay 권한 UX가 기기별로 다르다.
- foreground service 시작/유지 정책은 Android 버전별로 까다롭다.
- emulator success는 Gate A의 필요조건이지 충분조건이 아니다.

## 3. External Research Update

### Platform Constraints

Android:

- `UsageStatsManager`와 `UsageEvents.Event.ACTIVITY_RESUMED`는 foreground activity event 추적에는 맞지만, 연속적인 현재 앱 상태로 과신하면 안 된다.
- Android 14+는 foreground service type 선언과 start 조건이 더 엄격하다.
- Google Play Accessibility API 정책은 사용자 설정 변경, 앱 disable/uninstall 방지 같은 동작을 parental control 또는 enterprise management 맥락 밖에서 쓰는 것을 강하게 제한한다.

iOS:

- FamilyControls, ManagedSettings, DeviceActivity는 privacy-preserving Screen Time API다.
- Managed Settings와 Device Activity는 앱/웹 활동 모니터링과 shield에는 맞지만, 일반 소비자 앱이 전체 기기 잠금을 약속하는 방식은 피해야 한다.

제품 반영:

- 계속 `selected-app shielding/blocking`이라고 말한다.
- `전체 휴대폰 잠금`, `삭제 방지`, `권한 회수 방지`는 B2C MVP에서 금지한다.
- Android foreground service는 사용자가 앱에서 직접 시작하는 명시적 흐름을 유지한다.

### Competitive Constraints

조사한 서비스 기준:

- ScreenZen은 무료/무구독 포지션을 강하게 잡고 있어 단순 blocker 과금 명분을 낮춘다.
- Freedom은 무료 플랜에도 multi-device/app/site blocking을 제공하고, paid는 schedule/locked mode/cross-device value를 판다.
- one sec은 mobile + browser extension + Mac app으로 확장되어 desktop distraction까지 잡는다.
- Jomo/Roots는 strict mode, unlock action, reports, streaks, cheat day로 retention을 만든다.
- WakaTime은 coding history, commit/PR stats, export, team dashboards에 유료 가치를 둔다.

제품 반영:

- Android-only blocker를 Pro로 팔지 않는다.
- Pro의 중심은 `proof history`, `credit ledger`, `explainable scoring`, `browser/desktop companion`, `multi-source proof`다.
- 재미있는 개발자 톤은 entry/block overlay에 제한하고, ledger/report는 개발자 도구처럼 차분하게 만든다.

## 4. Gap Register

| 우선순위 | Gap | 왜 문제인가 | 보완 결정 |
| --- | --- | --- | --- |
| P0 | 실제 Android 기기 smoke 없음 | emulator와 실제 제조사 OS 제약은 다름 | 물리 기기 1대 이상에서 runbook 재수행 |
| P0 | 14일 dogfood TSV 없음 | 니즈/반복 사용/override 판단 불가 | 14일 수집 전 monetization/GitHub runtime 금지 |
| P0 | 실기기 foreground/service failure evidence 부족 | UsageStats/FGS/overlay edge case가 제조사별로 다를 수 있음 | runbook smoke에서 failure state와 recovery copy를 관찰 |
| P1 | desktop/browser companion 설계 없음 | 개발자 distraction은 PC/browser에 많음 | Chrome extension 또는 desktop helper spike 작성 |
| P1 | GitHub proof 공급량 미검증 | PR-only면 개인 개발자 activation이 낮을 수 있음 | WakaTime/IDE/commit batch fallback decision point 추가 |
| P1 | dogfood export redaction | package name/quest title 공유가 민감할 수 있음 | 완료. 원본 export와 redacted export를 앱에서 분리 |
| P1 | Android UI가 여전히 긴 단일 Activity | 유지보수 비용 증가 | smoke evidence 후 ViewModel/state 분리 |
| P2 | iOS entitlement 실검증 없음 | Screen Time API는 승인/실기기 변수가 큼 | Xcode/Developer setup 이후 별도 iOS spike |
| P2 | API/GitHub runtime 없음 | MVP-B로 가려면 HMAC/dedupe/ledger 필요 | Sprint 4 PR A부터 작게 구현 |

## 5. Revised Next Sequence

### Step 1: Android Real-Device Evidence

목표:

- 물리 Android 기기에서 `0분 차단`, `+5분 허용`, `60초 후 차감`, `emergency unlock`, `free day`를 확인한다.
- 제조사/OS/권한 화면/overlay 지연을 기록한다.

완료 기준:

- runbook smoke pass record 1개 이상.
- TSV export 1개 이상.
- Gate A/D가 pass 또는 명확한 fix list로 정리됨.

### Step 2: Desktop/Browser Companion Spike

목표:

- paid moat가 될 수 있는 최소 companion 범위를 정한다.

권장 범위:

- Chrome extension first.
- GitHub/proof ledger server 없이도 local mock credit을 읽는 dogfood-only prototype.
- target은 domain pattern list.
- 차단 방식은 redirect/interstitial page, not full browser lockdown.

### Step 3: Sprint 4 Webhook Security Foundation

목표:

- GitHub runtime을 scoring보다 보안 기초부터 연다.

범위:

- raw body HMAC validation.
- `X-GitHub-Delivery` dedupe.
- event allowlist.
- no raw diff storage.
- audit/event table scaffold.

### Step 4: Proof Supply Decision

목표:

- GitHub PR-only가 충분한지 확인한다.

결정:

- PR/MR가 충분하면 PR-centered scoring 유지.
- 부족하면 commit batch, WakaTime, IDE activity proof를 capped fallback으로 추가한다.

## 6. Do Not Build Yet

다음은 아직 금지한다.

- payment/subscription UI
- money stake/벌금
- parent/school/MDM
- AccessibilityService
- uninstall prevention
- whole-phone lock
- leaderboard
- full-diff LLM scoring
- marketing landing page

## 7. Source Notes

- Android UsageStatsManager and UsageEvents: https://developer.android.com/reference/android/app/usage/UsageStatsManager and https://developer.android.com/reference/android/app/usage/UsageEvents.Event.html
- Android foreground service requirements: https://developer.android.com/about/versions/14/changes/fgs-types-required
- Google Play sensitive APIs and Accessibility API policy: https://support.google.com/googleplay/android-developer/answer/9888170
- Apple ManagedSettings and DeviceActivity: https://developer.apple.com/documentation/managedsettings/connectionwithframeworks and https://developer.apple.com/documentation/deviceactivity
- Freedom pricing/value anchor: https://freedom.to/premium
- ScreenZen free/no-subscription anchor: https://www.screenzen.co/
- one sec browser/desktop expansion: https://one-sec.app/browser-extension
- Jomo pricing/unlock action anchor: https://jomo.so/pricing
- Roots pricing/Monk Mode anchor: https://www.getroots.app/pricing
- WakaTime developer proof/history anchor: https://wakatime.com/pricing and https://wakatime.com/features
