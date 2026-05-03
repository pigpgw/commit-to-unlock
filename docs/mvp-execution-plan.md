# MVP Execution Plan

문서 상태: v0.4
작성일: 2026-05-03
최종 정리: 2026-05-04
역할: 현재 MVP의 단일 실행 계획, 남은 작업 목록, 문서/코드 정리 기준

## 1. Current Decision

현재 제품은 계속 만든다. 단, 지금 만들 제품은 GitHub scoring 앱이 아니라 `Android local blocker dogfood MVP`다.

현재 최우선 목표:

```text
Android 실기기에서 selected app blocking이 실제로 쓸 만한지 검증한다.
```

아직 하지 않는다:

- GitHub OAuth/App/webhook runtime
- GitHub PR scoring runtime
- 결제, money stake, 벌금
- 부모/학교/MDM
- AccessibilityService
- Device Admin / uninstall prevention
- full-diff LLM scoring
- marketing site

## 2. Source Of Truth

앞으로 구현자는 아래 순서로 문서를 읽는다.

| 순서 | 문서 | 역할 |
| --- | --- | --- |
| 1 | [mvp-execution-plan.md](mvp-execution-plan.md) | 지금 무엇을 할지 결정하는 실행 계획 |
| 2 | [product-security-hardening-plan.md](product-security-hardening-plan.md) | 기획/보안/개인정보/platform policy hardening gate |
| 3 | [competitive-service-review.md](competitive-service-review.md) | 경쟁 서비스 조사와 차별화/수익화 gate |
| 4 | [android-dogfood-runbook.md](android-dogfood-runbook.md) | MVP-A 실기기 검증 절차 |
| 5 | [decision-log.md](decision-log.md) | 다시 판단하지 않을 제품/기술 결정 |
| 6 | [security-and-logic-review.md](security-and-logic-review.md) | 보안/정책/로직 gate |
| 7 | [github-sprint4-entry.md](github-sprint4-entry.md) | GitHub runtime 진입 기준 |
| 8 | [control-account-design.md](control-account-design.md) | 차단 범위, 계정, 탈퇴 UX |
| 9 | [app-design.md](app-design.md) | 전체 제품/기술 설계 |
| 10 | [proof-policy-mvp.md](proof-policy-mvp.md) | proof, quest, exception policy 상세 |

삭제된 과거 계획/스냅샷 문서는 git history에서만 확인한다. 충돌하면 이 문서와 decision log를 우선한다.

## 3. MVP Definition

### MVP-A: Android Local Enforcement Dogfood

목표: 개발자가 직접 Android 앱을 켜두고, 방해 앱 접근이 mock credit/policy에 따라 막히는지 확인한다.

완료 조건:

- Android debug APK build/lint/test 통과.
- 실제 Android 기기에서 Usage Access, Overlay, Notification 상태가 UI에 정확히 표시.
- selected target foreground + credit 0이면 overlay 표시.
- credit > 0이면 overlay 없음.
- target foreground 60초마다 1 mock minute 차감.
- free day, manual holiday, emergency unlock이 credit보다 우선 적용.
- dogfood TSV export와 analyzer가 Gate A/B/C/D 판단에 충분한 데이터를 제공.
- 14일 dogfood 기록 1세트 확보.

### MVP-B: Developer Proof Ledger

목표: GitHub PR/commit/review/CI proof가 credit ledger로 변환되고, 모바일 정책과 sync된다.

착수 조건:

- MVP-A Gate A/B/D 판단 기록.
- GitHub Sprint 4 security entry spec 완료.
- webhook HMAC 검증 설계와 테스트.
- delivery dedupe 설계.
- private repo raw diff 기본 저장 금지 정책.
- credit ledger schema와 mobile sync shape 확정.

## 4. Current Repo Status

| 영역 | 상태 | 판단 |
| --- | --- | --- |
| Android app | runnable local prototype + target guardrails | 현재 유일한 product surface |
| Android dogfood | runbook/log/export/analyzer/in-app review 있음 | 14일 실기기 데이터 필요 |
| Android UI | 긴 화면을 section helper와 pure text/time helper로 분리함 | 실기기 dogfood fix만 추가 |
| Shared policy | TS canonical + Android mirror + golden fixtures | 정책 drift 방지 기준 확보 |
| Scoring package | pure rules scaffold | 유지. runtime 연결 금지 |
| API | health-only, localhost/CORS-closed default | 유지. Sprint 4 전 auth/webhook 금지 |
| iOS | Swift source skeleton only | Xcode/entitlement 전까지 보류 |
| Docs | active docs only | 오래된 PRD/snapshot/reference 문서 삭제 |

## 5. MVP Closeout Status

현재 MVP-A는 `local Android prototype code-complete, dogfood-data-gated` 상태다.

완료:

- Android debug APK build/lint/test 기준선.
- Usage Access 기반 foreground 감지.
- selected target + credit 0 overlay.
- credit > 0 allow.
- 60초 foreground spend.
- weekday/time/manual holiday/free day/emergency unlock 정책.
- daily quest + mock proof free day.
- dogfood event log, TSV export, analyzer, in-app Gate review.
- target guardrail.
- public holiday placeholder UI 제거.
- time parser와 dogfood/quest text formatter 단위 테스트 추가.
- GitHub Sprint 4 security entry spec.
- product/security/competitive/account hardening docs.

부족한 것:

- 실제 Android 기기 smoke evidence.
- 14일 dogfood TSV 1세트.
- 제조사별 overlay/background 제한 확인.
- 실제 GitHub/WakaTime/IDE proof 빈도 확인.
- desktop/browser companion 설계.
- Sprint 4 webhook HMAC/dedupe runtime 구현.

## 6. Document Cleanup Plan

### Active

| 문서 | 역할 |
| --- | --- |
| `mvp-execution-plan.md` | 단일 실행 계획, MVP 현황, 남은 gate |
| `product-security-hardening-plan.md` | 신규 기능의 기획/보안/개인정보/platform hardening gate |
| `competitive-service-review.md` | 경쟁 서비스 조사, paid moat, desktop/browser companion, WakaTime fallback 판단 |
| `android-dogfood-runbook.md` | MVP-A 실기기 dogfood 절차와 gate decision template |
| `decision-log.md` | 다시 판단하지 않을 결정 |
| `security-and-logic-review.md` | Gate D와 보안 기준 |
| `github-sprint4-entry.md` | GitHub Sprint 4 진입 기준 |
| `control-account-design.md` | 기기별 차단 가능 범위, target 선택, 계정/탈퇴 UX |
| `app-design.md` | 제품/기술/UX 통합 설계 |
| `proof-policy-mvp.md` | proof, quest, exception policy |

### Deleted As Duplicates Or Stale Snapshots

삭제된 문서는 git history에 남긴다. 지금 repo에서는 실행 기준을 한 곳으로 모으기 위해 제거한다.

| 삭제 문서 | 이유 |
| --- | --- |
| `mvp-prd.md` | GitHub-first PRD라 현재 Android MVP-A와 우선순위가 다름 |
| `build-first-execution-plan.md` | 현재 실행 순서는 이 문서로 대체됨 |
| `mvp-progress-audit.md` | 일회성 snapshot이며 현재 현황은 이 문서의 MVP Closeout Status로 이동 |
| `repository-audit-and-cleanup.md` | 정리 기준은 이 문서와 README로 이동 |
| `android-sprint-1.1-design.md` | 완료된 스프린트 상세 문서. 남은 검증은 runbook으로 이동 |
| `mobile-credit-contract.md` | contract source는 `packages/shared/src/mobile-credit.ts`와 [app-design.md](app-design.md)에 유지 |
| `product-strategy-spec.md` | 유효한 포지션/패키징 판단은 [app-design.md](app-design.md)와 [competitive-service-review.md](competitive-service-review.md)에 흡수 |
| `design-research-and-ux-direction.md` | UI/톤 기준은 [app-design.md](app-design.md)에 흡수 |
| `market-needs-and-pivot-plan.md` | gate/pivot 판단은 이 문서와 [competitive-service-review.md](competitive-service-review.md)에 흡수 |

## 7. Code Cleanup Plan

### Keep

| 코드 | 이유 |
| --- | --- |
| `apps/android` | 현재 runnable MVP |
| `scripts/android-dogfood-*` | dogfood install/export/analyze에 필요 |
| `packages/shared` | policy/mobile contract source |
| `packages/scoring` | Sprint 4 rules-first scoring 후보 |
| `apps/api` | Sprint 4 API scaffold |
| `apps/ios` | iOS entitlement/Xcode 전 skeleton |

### Refactor

| 대상 | 문제 | 처리 |
| --- | --- | --- |
| `MainActivity.kt` | 여전히 큰 Android Activity | time/text helper는 분리 완료. 남은 분리는 실기기 smoke 이후 |
| `DogfoodEventStore.kt` | parser/export 핵심 | unit test 추가 완료. event type constants는 후순위 |
| TS/Kotlin policy mirror | drift 위험 | golden fixtures 추가 완료. 정책 변경 시 fixture 우선 갱신 |
| Android event type strings | 문자열 분산 | tests 후 constants/sealed class 검토 |

### Do Not Delete

| 대상 | 이유 |
| --- | --- |
| `packages/scoring` | Sprint 4 재사용 가능성이 높음 |
| `apps/api` | health-only라도 CI/API skeleton으로 필요 |
| `apps/ios` | Xcode 전 iOS Screen Time skeleton으로 필요 |

### Delete Only If Found

- tracked build artifacts
- generated JS/d.ts under source folders
- old webhook placeholder that scores without enrichment
- duplicate debug log store separate from dogfood event log

현재 main 기준으로 위 삭제 대상은 대부분 이미 제거되어 있다.

이번 cleanup에서 확인한 내용:

- tracked build artifact, generated JS/d.ts, tracked Android build output은 없다.
- ignored local artifacts는 `.gradle/`, `apps/android/build/`, package `dist/`, `node_modules/`로 남아 있을 수 있으나 repo에는 포함하지 않는다.
- `packages/scoring`, `apps/api`, `apps/ios`는 현재 runtime에는 연결하지 않지만 Sprint 4/iOS 준비 자산이라 삭제하지 않는다.
- Android `MainActivity.kt`는 여전히 크지만 section helper로 분리되어 있고, 지금은 실기기 검증 전 추가 리팩터보다 안정성이 우선이다.

## 8. Gates

| Gate | 상태 | 통과 기준 | 다음 작업 |
| --- | --- | --- | --- |
| A: Enforcement viability | needs_data | 실기기 smoke pass, overlay <= 2초, 권한 상태 정확 | runbook smoke + privacy UI |
| B: Dogfood need | needs_data | 14일 blocked attempt/override 데이터 | 14일 TSV collection |
| C: Proof supply | needs_data | 14일 실제 GitHub/WakaTime/IDE proof 빈도 | mock proof + dev activity note |
| D: Trust/privacy | needs_data | product/security hardening invariants, permission/privacy UI, retention/revoke/delete spec, webhook HMAC/dedupe spec | hardening gate + GitHub entry spec |
| E: Monetization | blocked | proof ledger 가치 확인 후 | 나중 |

## 9. Next PR Sequence

### PR 1: MVP execution plan

Status: complete.

Deliverables:

- `docs/mvp-execution-plan.md`
- README planning links 정리
- existing docs에 source-of-truth 연결

### PR 2: Dogfood runbook

Status: complete.

Deliverables:

- 14일 실기기 runbook
- smoke checklist
- TSV filename/archive rule
- Gate A/B/C/D decision template

### PR 3: DogfoodEventStore tests

Status: complete.

Deliverables:

- export header/column test
- legacy row parse test
- tab/newline sanitize test
- max 1,000 events test
- duplicate adjacent event behavior test

### PR 4: Policy golden fixtures

Status: complete.

Deliverables:

- shared JSON fixtures
- TypeScript policy fixture test
- Android Kotlin fixture test
- invalid timezone/overnight/free day/emergency cases

### PR 5: Android privacy and permission screen

Status: complete.

Deliverables:

- Usage Access disclosure
- Overlay disclosure
- dogfood data explanation
- export/clear explanation
- "local prototype, not tamper-proof" copy

### PR 6: Android dogfood review/Data Quality UI

Status: complete.

Deliverables:

- DogfoodReviewEngine pure Kotlin helper
- in-app Data Quality coverage
- in-app Gate A/B/C/D snapshot
- in-app recommendations
- Android unit tests

### PR 7: GitHub Sprint 4 entry spec

Status: complete as planning gate. Runtime implementation still waits for real-device Gate A/D smoke evidence.

Deliverables:

- GitHub App permissions
- webhook HMAC
- delivery dedupe
- PR enrichment
- credit ledger schema
- retention/revoke/delete
- Sprint 4 go/no-go checklist

### PR 8: Android UI section cleanup

Status: complete.

Deliverables:

- MainActivity section helpers
- Header / Permissions / Targets / Policy / Quest / Emergency / Credit / Monitor-Dogfood helpers
- no behavior change

### PR 9: Product and security hardening plan

Status: complete as planning gate.

Deliverables:

- 기획/보안/개인정보/platform policy hardening gate
- non-negotiable invariants
- threat model, data classification, retention defaults
- Android/iOS/GitHub/scoring/account hardening 기준
- immediate implementation queue

### PR 10: Competitive service review

Status: complete as planning gate.

Deliverables:

- blocker, earn-to-unlock, desktop blocker, developer productivity 서비스 비교
- paid moat를 proof ledger, cross-device sync, browser/desktop companion으로 재정의
- Android-only blocker 과금 금지 기준 보강
- desktop/browser companion과 WakaTime/IDE proof spike를 다음 설계 후보로 승격

### PR 11: Android target guardrails

Status: complete.

Deliverables:

- target normalization before save/read
- own package, empty target, duplicate target rejection
- launcher/settings/permission-controller/core service denylist draft
- rejected-target dogfood events
- Android unit tests

### PR 12: MVP cleanup and closeout audit

Status: complete.

Deliverables:

- stale reference docs removed
- active source-of-truth reduced to current MVP docs
- MVP closeout status and missing work consolidated
- code cleanup scan documented

### PR 13: Android maintenance cleanup

Status: complete.

Deliverables:

- public holiday placeholder UI removed from Android local MVP
- time input parsing extracted and unit-tested
- dogfood event / quest summary text formatting extracted and unit-tested
- recent package suggestions filtered through target guardrails

### PR 14: CI runtime cleanup

Status: complete.

Deliverables:

- Android CI setup action updated to Node 24 runtime
- unnecessary forced JavaScript action runtime override removed
- README branch examples aligned with the repo task-branch convention

## 10. Work Rules

- 한 PR에 unrelated scope를 섞지 않는다.
- 코드 삭제는 테스트/문서 기준 없이 하지 않는다.
- Android behavior 변경 전에는 unit test를 먼저 보강한다.
- Sprint 4 전까지 mobile mock credit을 API와 연결하지 않는다.
- GitHub sync는 [product-security-hardening-plan.md](product-security-hardening-plan.md)의 Gate D와 [github-sprint4-entry.md](github-sprint4-entry.md)를 통과하기 전까지 구현하지 않는다.
- 로그인/회원가입/로그아웃/회원탈퇴 구현은 [control-account-design.md](control-account-design.md)의 삭제/권한/target guard를 먼저 만족해야 한다.
- Android-only local blocker를 유료 제품으로 포장하지 않는다. paid work는 [competitive-service-review.md](competitive-service-review.md)의 proof ledger/cross-device moat 기준을 따른다.
- 결제/부모/학교/MDM/money stake는 계속 금지한다.

## 11. Immediate Next Action

이 PR 이후 즉시 할 일:

```text
real-device Android dogfood smoke, desktop/browser companion spike, then Sprint 4 PR A: Webhook Security Foundation
```

이유: runbook, event store tests, policy golden fixtures, 권한/개인정보 disclosure, in-app Data Quality/Gate review, GitHub Sprint 4 entry spec, product/security hardening gate, competitive service review, Android target guardrails는 완료됐다. 이제 실제 기기에서 Gate A/D smoke evidence를 만든다. 그 다음 Freedom/Cold Turkey/FocusMe류가 보여준 desktop/browser paid moat를 우리 proof ledger에 연결하는 spike를 작성한 뒤 [github-sprint4-entry.md](github-sprint4-entry.md)의 PR A부터 구현한다.
