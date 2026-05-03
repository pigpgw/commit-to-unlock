# Commit-to-Unlock Build-First 실행 계획

결정: 인터뷰/설문 없이 만든다.  
전략: 고객 검증을 말로 하지 않고, 먼저 `로컬 모바일 차단 -> mock credit -> selected app shield/block` 루프를 실제 기기/에뮬레이터에서 검증한다. GitHub scoring은 모바일 차단 가능성이 확인된 뒤 재개한다.

제품/기술/UX의 기준 설계는 [app-design.md](app-design.md)를 따른다. 제품 전략/사업 패키징은 [product-strategy-spec.md](product-strategy-spec.md)를 따른다. 구현 판단은 [decision-log.md](decision-log.md)를 우선 확인한다. 시장/니즈/피벗 판단은 [market-needs-and-pivot-plan.md](market-needs-and-pivot-plan.md)를 따른다. Android 다음 구현 단위는 [android-sprint-1.1-design.md](android-sprint-1.1-design.md)를 따른다. 이 문서는 실행 순서와 milestone 기준을 관리한다.

## 1. 바뀐 원칙

기존 Phase 0의 인터뷰, 설문, fake-door 검증은 제외한다. 첫 검증 단위는 GitHub scoring simulator가 아니라 로컬 모바일 차단 프로토타입이다.

이 방식의 장점:

- 시간을 아낀다.
- 실제 모바일 플랫폼에서 selected-app 차단이 가능한지 바로 확인한다.
- “이 제품을 쓰겠다”는 말보다 “mock credit이 0일 때 방해 앱이 막히는지”를 본다.

이 방식의 리스크:

- 사용자가 원하지 않는 걸 너무 열심히 만들 수 있다.
- GitHub scoring 리스크를 늦게 발견할 수 있다.
- private repo 권한 거부 같은 구매 전 리스크는 Sprint 4 이후에 드러난다.

그래서 1차 목표는 완성 앱이 아니라 `모바일 차단 가능성`을 가장 빨리 확인하는 것이다.

## 2. MVP 빌드 순서

### Milestone 1: Android Local Blocking Prototype

목표: GitHub/API 없이 Android에서 선택 앱 감지와 mock credit 기반 차단 화면을 검증한다.

기능:

- Usage Access 권한 안내
- Display over other apps 권한 안내
- 차단할 Android package name 수동 입력
- foreground app 감지
- mock credit local state 저장
- mock credit이 0이면 overlay 차단 화면 표시
- mock credit이 있으면 접근 허용

성공 기준:

- Gradle Wrapper 기준 `:apps:android:assembleDebug` 통과
- 앱 재시작 후 `remainingMinutes`, `blockedTargets`, `strictMode`, `lastUpdatedAt` 유지
- Usage Access 또는 overlay 권한이 없으면 권한 안내 상태가 표시됨
- blocked target이 foreground이고 credit이 0이면 overlay가 표시됨
- AccessibilityService를 쓰지 않음

### Milestone 2: iOS Local Shield Preparation

목표: Xcode 설치 전까지 iOS Screen Time API 타깃 구조와 소스 골격을 준비한다.

기능:

- SwiftUI main app source skeleton
- local mock `CreditState`
- FamilyControls authorization controller skeleton
- ManagedSettings shield apply/clear controller skeleton
- extension target design 문서
- Family Controls entitlement 신청 항목 정리

성공 기준:

- `apps/ios`에 Xcode 프로젝트로 옮길 수 있는 Swift source와 target design이 존재
- iOS는 실제 Xcode 설치 후 simulator/기기 build로 검증

### Milestone 3: GitHub Scoring Simulator

목표: 모바일 차단 없이 GitHub 활동을 크레딧으로 바꾸는 서버/웹 루프를 만든다.

주의: Milestone 3은 자동 착수하지 않는다. 먼저 [market-needs-and-pivot-plan.md](market-needs-and-pivot-plan.md)의 Gate B/C를 확인한다. 14일 dogfood에서 반복 사용이 약하거나 자연스러운 scorable dev event가 부족하면 GitHub PR-only 대신 WakaTime/IDE/desktop-browser proof channel을 먼저 검토한다.

기능:

- GitHub App 설치
- repo allowlist
- webhook receiver
- pull_request, push, check_run/check_suite, pull_request_review 이벤트 처리
- PR files/commits/reviews/checks enrichment
- feature vector 생성
- rules-first scoring
- credit ledger 적립
- activity feed
- scoring explanation UI

성공 기준:

- 실제 GitHub repo에서 PR merge 후 1분 내 scoring decision 생성
- 같은 webhook 재전송에도 중복 적립 없음
- generated/lockfile/whitespace-only 감점 동작
- score decision이 사람이 읽을 수 있는 이유를 포함

### Milestone 4: Credit Policy Engine

목표: 단순 점수가 아니라 사용 가능한 크레딧 장부를 만든다.

기능:

- provisional vs confirmed credit
- daily cap
- credit spend
- clawback
- override
- appeal placeholder
- suspicious activity flags

성공 기준:

- commit batch는 provisional cap 안에서만 적립
- merged PR은 confirmed credit으로 승격
- revert/duplicate patch는 clawback 후보가 됨
- `/credits/today`가 모바일 앱이 바로 쓸 수 있는 형태로 응답

### Milestone 5: iOS Enforcement Spike

목표: App Store 출시 전, FamilyControls/ManagedSettings로 selected-app shield가 가능한지 실제 기기에서 확인한다.

기능:

- Family Controls authorization
- FamilyActivityPicker
- selected app/domain token 저장
- ManagedSettings shield 적용/해제
- credit sync mock
- shield screen copy

성공 기준:

- 선택한 앱이 shield된다.
- credit이 있으면 shield가 해제된다.
- credit이 0이면 다시 shield된다.
- entitlement/TestFlight/App Review 리스크가 문서화된다.

### Milestone 6: Android Enforcement Hardening

목표: Sprint 1의 Android prototype을 Google Play 정책에 맞게 정리한다.

기능:

- Usage Access permission 문구 정리
- overlay permission 문구 정리
- foreground app detection 안정화
- selected package policy
- block/interruption screen
- credit sync mock

성공 기준:

- 선택 앱 접근을 감지한다.
- credit이 없을 때 정책 화면으로 유도한다.
- AccessibilityService를 사용하지 않는 1차 구현이 유지된다.

### Milestone 7: Private Alpha

목표: 본인과 소수 지인 계정으로 end-to-end를 돌린다.

기능:

- GitHub App production/private install
- backend deploy
- Postgres/Redis
- iOS 또는 Android alpha build
- basic analytics
- privacy/delete/revoke

성공 기준:

- 7일 동안 매일 scoring/ledger/enforcement가 깨지지 않음
- 본인 실제 사용에서 override가 과도하게 발생하지 않음
- scoring이 억울한 케이스를 최소 20개 수집하고 rule을 보정

## 3. 권장 스택

### Backend

- TypeScript
- Node.js
- Fastify
- PostgreSQL
- Prisma
- Redis + BullMQ
- Zod
- Vitest

이유: GitHub webhook/API 처리, background jobs, JSON feature extraction에 적합하고, MVP 속도가 빠르다.

### Web

- Next.js
- Tailwind CSS
- shadcn/ui
- Recharts

역할: GitHub 연결, activity feed, scoring explanation, policy 설정.

### Mobile

iOS:

- Swift/SwiftUI native
- FamilyControls
- DeviceActivity
- ManagedSettings
- ManagedSettingsUI

Android:

- Kotlin native
- UsageStatsManager
- foreground service
- overlay block screen
- AccessibilityService 제외

React Native/Flutter는 초기 UI 속도에는 좋지만, iOS Screen Time extension과 Android 정책 edge case가 핵심이므로 enforcement spike는 native가 낫다.

## 4. 첫 저장소 구조

```text
apps/
  api/
  web/
  ios/
  android/
packages/
  scoring/
  shared/
  db/
docs/
```

첫 구현은 `apps/android`, `apps/ios`, `packages/shared`부터 시작한다. API/scoring은 유지하되 Sprint 4 전까지 모바일과 연결하지 않는다.

## 5. 1차 백로그

### Android

- Gradle wrapper bootstrap
- native Kotlin app
- Usage Access permission guide
- overlay permission guide
- blocked package input
- local `CreditState` SharedPreferences store
- foreground app polling with UsageStatsManager
- blocking overlay when mock credit is 0
- monitor foreground service
- current foreground package 표시
- bounded debug log
- credit 0 reset button
- strictMode일 때 overlay shortcut 제한
- dogfood event logging/export
- local spend engine
- block overlay copy/state refinement

### iOS

- SwiftUI source skeleton
- local `CreditState`
- FamilyControls authorization skeleton
- ManagedSettings shield controller skeleton
- target/extension design docs
- entitlement checklist

### API

- `POST /webhooks/github`
- webhook signature verification
- inbound event dedupe by delivery id
- event queue
- GitHub App installation token 발급
- PR enrichment job
- score decision 생성
- credit ledger write
- `GET /activity/feed`
- `GET /credits/today`

### Scoring

- file category classifier
- generated/vendor/lockfile detector
- whitespace-only detector
- normalized patch hash
- issue link detector
- test signal detector
- CI status summarizer
- scoring rubric v0
- explanation generator without LLM

### Web

- landing 없이 dashboard-first
- GitHub install/start page
- repo selection
- activity feed
- score decision detail
- today credit panel
- policy placeholder

### DB

- users
- github_installations
- repositories
- inbound_events
- pull_requests
- feature_vectors
- score_decisions
- credit_ledger
- risk_flags

## 6. 지금 당장 하지 않을 것

- 인터뷰
- 설문
- fake-door 랜딩
- 결제
- 학교/부모/미성년자
- GitLab/Bitbucket
- LLM scoring
- 금전 스테이크
- leaderboard
- MDM

## 7. 빌드 중 판단 기준

계속 만든다:

- Android selected-app overlay prototype이 실제 기기/에뮬레이터에서 동작한다.
- iOS FamilyControls entitlement와 Xcode 프로젝트 전환 경로가 막히지 않는다.
- mock credit contract가 Android/iOS 양쪽에서 같은 의미로 유지된다.
- 이후 GitHub scoring을 붙일 API shape가 명확하다.
- 14일 dogfood에서 monitor enabled day가 8일 이상이고 override가 주 3회 이하이다.
- 14일 동안 자연스러운 scorable dev event가 5개 이상이다. PR-only가 부족하면 commit batch/WakaTime/IDE channel을 추가한다.

방향 전환:

- UsageStats 기반 foreground 감지가 불안정하면 browser extension/web blocker를 먼저 출시한다.
- Android overlay가 정책상 위험하면 interruption-only UX로 낮춘다.
- PR 중심이 너무 느리면 commit batch provisional credit 비중을 높인다.
- GitHub App 권한 부담이 크면 public repo mode와 WakaTime mode를 검토한다.
- 사용자가 제품을 “비싼 차단 앱”으로 인식하면 local one-time + cloud scoring Pro로 가격 구조를 바꾼다.
- earn-to-unlock 언어가 처벌처럼 느껴지면 proof ledger/report 중심으로 낮춘다.

중단:

- iOS/Android 둘 다 정책상 핵심 enforcement가 불가능하다.
- GitHub scoring이 너무 쉽게 조작되고 rule로 막기 어렵다.
- 실제 사용에서 차단보다 override가 기본 행동이 된다.

## 8. 즉시 실행 순서

1. 저장소 생성 산출물 정리
2. Android native Kotlin app scaffold
3. Gradle wrapper bootstrap
4. Android local credit state
5. Android UsageStats foreground detection
6. Android overlay blocking screen
7. iOS SwiftUI/FamilyControls source skeleton
8. mobile credit contract 문서화
9. repo build/test/typecheck 기준선 확인

## 9. 보강된 다음 실행 순서

현재 저장소는 1차 Android prototype과 Sprint 1.1 hardening이 들어간 상태다. 다음 개발은 아래 순서로 간다.

1. Android dogfood event logging
2. Android local spend engine
3. Android block overlay copy/state refinement
4. 14일 dogfood 기준으로 Gate 1/2 판단
5. iOS Xcode project/entitlement 준비
6. GitHub scoring 재개 또는 WakaTime/IDE proof spike 선택
