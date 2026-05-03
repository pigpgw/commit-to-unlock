# MVP Execution Plan

문서 상태: v0.2
작성일: 2026-05-03
최종 정리: 2026-05-03
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
| 2 | [android-dogfood-runbook.md](android-dogfood-runbook.md) | MVP-A 실기기 검증 절차 |
| 3 | [decision-log.md](decision-log.md) | 다시 판단하지 않을 제품/기술 결정 |
| 4 | [security-and-logic-review.md](security-and-logic-review.md) | 보안/정책/로직 gate |
| 5 | [github-sprint4-entry.md](github-sprint4-entry.md) | GitHub runtime 진입 기준 |
| 6 | [app-design.md](app-design.md) | 전체 제품/기술 설계 |
| 7 | [proof-policy-mvp.md](proof-policy-mvp.md) | proof, quest, exception policy 상세 |

나머지 문서는 reference다. 충돌하면 이 문서와 decision log를 우선한다.

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
| Android app | runnable local prototype | 현재 유일한 product surface |
| Android dogfood | runbook/log/export/analyzer/in-app review 있음 | 14일 실기기 데이터 필요 |
| Android UI | 긴 화면을 section helper로 분리함 | 실기기 dogfood fix만 추가 |
| Shared policy | TS canonical + Android mirror + golden fixtures | 정책 drift 방지 기준 확보 |
| Scoring package | pure rules scaffold | 유지. runtime 연결 금지 |
| API | health-only, localhost/CORS-closed default | 유지. Sprint 4 전 auth/webhook 금지 |
| iOS | Swift source skeleton only | Xcode/entitlement 전까지 보류 |
| Docs | source-of-truth 정리됨 | reference 문서는 실행 기준이 아님 |

## 5. Document Cleanup Plan

### Active

| 문서 | 액션 |
| --- | --- |
| `mvp-execution-plan.md` | 신규 단일 실행 계획 |
| `android-dogfood-runbook.md` | MVP-A 실기기 dogfood 절차와 gate decision template |
| `decision-log.md` | 결정만 유지 |
| `security-and-logic-review.md` | Gate D와 보안 기준 유지 |
| `github-sprint4-entry.md` | GitHub Sprint 4 진입 기준 |
| `app-design.md` | 기술/제품 설계 유지 |
| `proof-policy-mvp.md` | 정책 상세 유지 |

### Reference

| 문서 | 이유 |
| --- | --- |
| `product-strategy-spec.md` | 제품/사업 포지션 참고 |
| `design-research-and-ux-direction.md` | UI/톤 참고 |
| `mobile-credit-contract.md` | Android/iOS/API contract 참고 |
| `android-sprint-1.1-design.md` | Android 구현 상세 참고 |
| `market-needs-and-pivot-plan.md` | 시장/피벗 참고 |
| `repository-audit-and-cleanup.md` | 정리 기준 참고 |
| `mvp-progress-audit.md` | 2026-05-03 snapshot 참고 |
| `mvp-prd.md` | GitHub-backed MVP reference. Sprint 4 전까지 실행 기준 아님 |

### Do Not Delete Yet

현재는 문서를 삭제하지 않는다. 삭제보다 먼저 README와 source-of-truth를 정리하고, 중복이 실제 구현 혼선을 만든 문서만 archive 후보로 둔다.

Archive 후보:

- `mvp-prd.md`: GitHub-first PRD라 현재 MVP-A와 우선순위가 다름.
- `build-first-execution-plan.md`: 상세 milestone 기록으로 남기되, 이 문서가 실행 기준을 대체함.
- `mvp-progress-audit.md`: 현재 snapshot이라 계속 갱신하지 않음.

## 6. Code Cleanup Plan

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
| `MainActivity.kt` | 700라인 이상, UI/logic 혼재 | 테스트 보강 후 section renderer/helper로 분리 |
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

## 7. Gates

| Gate | 상태 | 통과 기준 | 다음 작업 |
| --- | --- | --- | --- |
| A: Enforcement viability | needs_data | 실기기 smoke pass, overlay <= 2초, 권한 상태 정확 | runbook smoke + privacy UI |
| B: Dogfood need | needs_data | 14일 blocked attempt/override 데이터 | 14일 TSV collection |
| C: Proof supply | needs_data | 14일 실제 GitHub/WakaTime/IDE proof 빈도 | mock proof + dev activity note |
| D: Trust/privacy | needs_data | permission/privacy UI, retention/revoke/delete spec, webhook HMAC/dedupe spec | privacy UI + GitHub entry spec |
| E: Monetization | blocked | proof ledger 가치 확인 후 | 나중 |

## 8. Next PR Sequence

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

## 9. Work Rules

- 한 PR에 unrelated scope를 섞지 않는다.
- 코드 삭제는 테스트/문서 기준 없이 하지 않는다.
- Android behavior 변경 전에는 unit test를 먼저 보강한다.
- Sprint 4 전까지 mobile mock credit을 API와 연결하지 않는다.
- GitHub sync는 Gate D를 통과하기 전까지 구현하지 않는다.
- 결제/부모/학교/MDM/money stake는 계속 금지한다.

## 10. Immediate Next Action

이 PR 이후 즉시 할 일:

```text
real-device Android dogfood smoke, then Sprint 4 PR A: Webhook Security Foundation
```

이유: runbook, event store tests, policy golden fixtures, 권한/개인정보 disclosure, in-app Data Quality/Gate review, GitHub Sprint 4 entry spec은 완료됐다. 이제 실제 기기에서 Gate A/D smoke evidence를 만든 뒤 [github-sprint4-entry.md](github-sprint4-entry.md)의 PR A부터 구현한다.
