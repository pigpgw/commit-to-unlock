# Proof Policy MVP

문서 상태: v0.1
작성일: 2026-05-03
역할: `개발 증거 -> 크레딧 -> 선택 앱 차단/해제` 정책의 MVP 기준 문서

## 1. Product Decision

Commit-to-Unlock은 todo 앱이 아니다.

금지:

- 사용자가 “했다”고 체크했다고 시간이 열리는 구조
- 수동 체크리스트 완료만으로 unlock 되는 구조
- AI가 막연히 “열심히 했다”고 판정하는 구조

허용:

- commit, PR, review, CI, issue link 같은 개발 증거가 credit을 만든다.
- 사용자가 오늘 할 일을 등록할 수는 있지만, 완료 판정은 proof-backed여야 한다.
- 예외 상황은 별도 정책으로 다루고, 모두 기록한다.

핵심 문장:

> 수동 todo는 계획이고, unlock 권한은 개발 증거와 정책 예외만 가진다.

## 2. MVP User Story

사용자는 개발자다. YouTube, SNS, Reddit, 게임, 뉴스 앱을 줄이고 싶다.

하루 흐름:

1. 앱에 들어오면 “개발자이신가요?” 게이트를 본다.
2. 차단할 앱을 선택한다.
3. 적용 요일과 시간을 정한다.
4. 오늘의 개발 퀘스트를 등록한다.
5. GitHub commit/PR/review 또는 local mock proof가 들어오면 credit이 생긴다.
6. 차단 앱을 열면 policy engine이 `allowed`, `blocked`, `free_day`, `emergency_unlock` 중 하나를 결정한다.
7. 필수 퀘스트가 모두 proof-backed completed가 되면 그날은 free day가 된다.
8. 예외 상황에서는 emergency unlock을 제한된 횟수로 쓸 수 있다.

## 3. Policy Resolution Order

차단 여부는 아래 순서로 결정한다. 위 조건이 먼저 맞으면 아래 조건은 보지 않는다.

```text
1. 앱 자신의 package인가? -> allow
2. blocked target이 아닌가? -> allow
3. 정책 적용 요일/시간이 아닌가? -> allow
4. 오늘이 정책상 휴일인가? -> allow
5. freeUntil이 현재보다 미래인가? -> allow
6. active emergency unlock이 있는가? -> allow
7. remainingMinutes > 0인가? -> allow and spend
8. 그 외 -> block
```

이 순서를 고정하는 이유:

- 요일/휴일/free day는 credit보다 상위 정책이다.
- emergency unlock은 credit이 없어도 열어주지만 audit 대상이다.
- credit spend는 마지막 허용 조건이다.

현재 TypeScript 순수 함수 구현은 `packages/shared/src/policy.ts`의 `evaluatePolicyDecision`이다. Android prototype은 `apps/android/src/main/java/com/commitunlock/prototype/PolicyDecisionEngine.kt`에서 같은 reason code와 우선순위를 미러링한다. Android/iOS/API는 같은 함수 또는 같은 reason code를 따라야 한다.

Decision reason:

| Reason | Allowed | Credit spend | 의미 |
| --- | --- | --- | --- |
| `own_app` | yes | no | 앱 자기 자신은 절대 차단하지 않는다. |
| `target_not_blocked` | yes | no | 선택 차단 대상이 아니다. |
| `inactive_weekday` | yes | no | 오늘 요일에는 정책이 적용되지 않는다. |
| `outside_active_time` | yes | no | 현재 시간이 적용 시간 밖이다. |
| `manual_holiday` | yes | no | 사용자가 오늘을 휴일 처리했다. |
| `public_holiday` | yes | no | 공휴일이고 공휴일 적용이 꺼져 있다. |
| `free_day` | yes | no | 오늘 required quest를 proof-backed 완료했다. |
| `emergency_unlock` | yes | no | 비상 해제가 활성화되어 있다. |
| `credit_available` | yes | yes | credit이 있으므로 허용하고 사용량에 따라 차감한다. |
| `credit_empty` | no | no | 차단 대상이며 적용 중이고 credit/예외가 없다. |

요일과 시간은 `PolicyState.timezone` 기준으로 평가한다. `freeUntil`, emergency unlock 시간은 ISO timestamp로 저장하고 절대 시간 비교를 한다.

## 4. Credit Earning

MVP의 실제 production earning은 GitHub proof가 기준이다. Android prototype에서는 GitHub가 붙기 전까지 mock proof로 같은 구조를 실험한다.

| Proof | Credit | 성격 | 비고 |
| --- | ---: | --- | --- |
| commit batch | 5-15분 | provisional | 작은 개발 리듬 보상 |
| PR opened/updated | 10-25분 | provisional | PR 중심 루프 진입 |
| PR merged | 30-60분 | confirmed | 가장 강한 credit |
| review/comment/resolution | 5-20분 | provisional/confirmed | 협업 신호 |
| tests/CI/docs 포함 | bonus | modifier | 단독 proof가 아니라 보정 |
| whitespace-only/lockfile-only | 0분 | rejected | 게임화 방지 |
| generated/vendor-heavy | capped | risk | 상한 제한 |
| bot authored | 0분 | rejected | 자동 작업 제외 |

MVP에서는 raw score를 노출하지 않는다. 사용자에게는 `몇 분`, `왜`, `provisional/confirmed`, `risk flag`만 보여준다.

## 5. Daily Quest

Daily Quest는 todo가 아니라 proof를 묶는 label이다.

예:

- `Android overlay copy 정리`
- `README 설치 문서 보강`
- `auth refresh PR 만들기`
- `테스트 3개 추가`

상태:

| Status | 의미 | unlock 영향 |
| --- | --- | --- |
| `planned` | 사용자가 오늘 할 일을 적음 | 없음 |
| `proof_seen` | commit/PR/review 후보가 연결됨 | credit 후보 |
| `completed` | proof rule을 통과함 | required quest 완료 |
| `rejected` | proof가 너무 약하거나 악용 위험 | 없음 |

규칙:

- `required=true`인 quest는 proof-backed `completed`가 되어야 한다.
- 수동 체크는 note일 뿐 unlock에 직접 영향이 없다.
- 모든 required quest가 completed이면 `freeUntil = 오늘 23:59:59`를 설정할 수 있다.
- free day는 ledger/event log에 남긴다.

현재 Android prototype 구현:

- `DailyQuestStore`가 오늘 날짜 기준 quest를 SharedPreferences에 저장한다.
- `Add daily quest plan`은 `planned` 상태만 만들며 unlock에 영향을 주지 않는다.
- `Complete next quest with mock proof`는 required quest를 우선 `completed`로 바꾸고 `proofType=mock`을 기록한다.
- required quest가 1개 이상이고 모두 `completed`이면 `freeUntil`을 해당 timezone의 자정 직전까지 설정한다.
- 모든 추가/완료/free day 부여는 dogfood event로 남긴다.

## 6. Free Day

Free day는 “오늘 할 일을 다 했으니 전체 프리” 정책이다.

권장 기본값:

- 기본 OFF
- 사용자가 켤 수 있음
- required quest 최소 1개 필요
- 수동 체크만으로는 free day 불가
- free day는 당일 timezone 기준 자정까지

Free day가 켜져도 유지할 것:

- event logging
- dogfood export
- weekly review
- override/freeday reason

Free day가 해도 안 되는 것:

- 앱 권한 변경
- 데이터 삭제
- GitHub 연결 해제
- blocked target 목록 삭제

## 7. Emergency Unlock

강제 해제는 필요하다. 없으면 사용자는 앱을 삭제하거나 권한을 꺼버린다.

MVP 정책:

| 항목 | 기본값 |
| --- | --- |
| duration | 5분, 15분, 30분 |
| daily limit | 3회 |
| weekly limit | 10회 |
| reason | 필수 |
| strict mode | 30분 금지, 확인 문구 추가 |
| logging | 필수 |

카피:

- `비상 탈출입니다. 미래의 내가 로그를 봅니다.`
- `이건 실패가 아니라 예외입니다. 다만 기록은 남습니다.`

Emergency unlock은 부끄럽게 만들지 않는다. 대신 반복 사용이 많으면 다음 날 정책을 제안한다.

## 8. Weekday, Weekend, Holiday Policy

MVP에는 적용 요일을 반드시 넣는다.

기본값:

- Monday-Friday: ON
- Saturday-Sunday: OFF
- time window: all day
- public holidays: local Android MVP에서는 자동 판정과 UI 설정 없음
- manual holiday today: OFF

설정:

| Setting | 의미 |
| --- | --- |
| `activeWeekdays` | 정책을 적용할 요일. 1=Monday, 7=Sunday |
| `activeFrom` | 적용 시작 시간. optional |
| `activeUntil` | 적용 종료 시간. optional |
| `applyOnPublicHolidays` | future API/server policy에서 공휴일에도 적용할지. Android local MVP UI에는 노출하지 않음 |
| `manualHolidayToday` | 오늘 하루 휴일 처리 |
| `timezone` | 날짜/요일/freeUntil 계산 기준 |

공휴일 MVP 결정:

- 자동 공휴일 API와 Android UI 설정은 MVP에서 제외한다.
- 사용자가 `오늘 휴일 처리`를 누를 수 있게 한다.
- v1에서 locale/country 기반 holiday table 또는 calendar provider를 붙인다.

이유:

- 공휴일 자동화는 국가/지역/대체공휴일/회사 휴일 때문에 초기에 복잡하다.
- MVP에서는 “정책이 오늘 적용되느냐”를 검증하는 게 더 중요하다.

## 9. Suggested Data Model

```ts
export interface CreditState {
  remainingMinutes: number;
  freeUntil?: string;
  strictMode: boolean;
  lastUpdatedAt: string;
}

export interface PolicyState {
  blockedTargets: string[];
  activeWeekdays: number[];
  activeFrom?: string;
  activeUntil?: string;
  applyOnPublicHolidays: boolean;
  manualHolidayToday: boolean;
  timezone: string;
  emergencyUnlocksToday: number;
}

export interface DailyQuest {
  id: string;
  title: string;
  required: boolean;
  proofType: "commit" | "pull_request" | "review" | "mock";
  proofRef?: string;
  status: "planned" | "proof_seen" | "completed" | "rejected";
  createdAt: string;
  completedAt?: string;
}

export interface EmergencyUnlock {
  id: string;
  durationMinutes: 5 | 15 | 30;
  reason: string;
  startedAt: string;
  expiresAt: string;
}
```

## 10. MVP Build Plan

### Sprint P1: Local Policy Engine

- `PolicyState` local store
- active weekday check
- manual holiday today
- freeUntil check
- emergency unlock check
- policy decision function

Acceptance:

- blocked target이 아니면 allow
- inactive weekday면 allow
- manual holiday today면 allow
- timezone 기준으로 요일/시간을 평가
- emergency unlock과 free day는 credit을 차감하지 않음
- credit 0이고 예외가 없으면 block

### Sprint P2: Android Policy UI

- active weekdays toggle
- manual holiday today toggle
- emergency unlock buttons
- policy status summary

현재 Android prototype 구현:

- active weekdays checkbox
- optional activeFrom/activeUntil input
- manual holiday today toggle
- mock free day until midnight
- emergency unlock 5/15/30분, reason 필수, 일 3회/주 10회 제한
- strict mode에서는 30분 emergency unlock 금지
- overlay와 dogfood log에 policy reason 기록

Acceptance:

- 주말 OFF면 weekend에는 overlay가 뜨지 않는다.
- emergency unlock 중에는 overlay가 뜨지 않는다.
- unlock 만료 후 다시 block된다.

### Sprint P3: Daily Quest Local Prototype

- daily quest add/list
- required flag
- mock proof complete
- all required completed -> freeUntil today end

현재 Android prototype 구현:

- daily quest title input
- required flag
- mock proof completion button
- today-only local store
- quest summary
- dogfood summary counters

Acceptance:

- 수동 planned 상태만으로 free day가 되지 않는다.
- mock proof complete 후 required quest가 모두 completed면 free day가 된다.
- free day 상태가 dogfood event에 기록된다.

### Sprint P4: GitHub Proof Adapter

- commit/PR/review proof event model
- quest proof matching
- credit ledger entry
- rejected proof reasons

Acceptance:

- commit/PR proof가 credit을 만든다.
- todo click만으로는 credit이 생기지 않는다.
- weak proof는 rejected로 남는다.

## 11. Security And Safety Boundaries

절대 하지 않는다:

- 장난 UX가 데이터를 삭제하게 만들기
- `아니오` 선택으로 설정/권한/크레딧 삭제하기
- GitHub repo, branch, issue, PR을 앱이 임의로 수정/삭제하기
- 차단 우회를 막기 위해 AccessibilityService를 남용하기
- 앱 삭제 방지를 약속하기

해야 한다:

- 모든 override/free day/proof decision을 기록한다.
- private repo raw diff 저장은 opt-in 전까지 하지 않는다.
- 권한 요청 화면은 장난 카피보다 정확한 설명을 우선한다.
- “개발자 전용”은 브랜드 톤이지 실제 보안 인증이 아님을 문서화한다.

## 12. Open Questions

- free day가 너무 강하면 credit ledger 사용이 줄어드는가?
- emergency unlock daily limit 기본값은 3회가 적절한가?
- 주말 기본 OFF가 개발자 self-control 니즈와 맞는가?
- public holiday 자동화는 한국/미국 중 어디부터 붙일 것인가?
- commit batch proof가 게임화되면 WakaTime/IDE proof를 먼저 붙일 것인가?
