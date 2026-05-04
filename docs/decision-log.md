# Commit-to-Unlock Decision Log

문서 상태: v0.6
목적: 구현자가 다시 판단하지 않아도 되는 제품/기술 결정 기록

## Current Source Of Truth

| 주제 | 기준 문서 |
| --- | --- |
| 현재 MVP 실행 계획 | [mvp-execution-plan.md](mvp-execution-plan.md) |
| 기획/보안 hardening gate | [product-security-hardening-plan.md](product-security-hardening-plan.md) |
| 경쟁 서비스 조사/차별화 | [competitive-service-review.md](competitive-service-review.md) |
| 현재 부족한 것 register | [mvp-gap-analysis.md](mvp-gap-analysis.md) |
| Android dogfood 실행 절차 | [android-dogfood-runbook.md](android-dogfood-runbook.md) |
| 전체 제품/기술 설계 | [app-design.md](app-design.md) |
| proof/quest/policy MVP | [proof-policy-mvp.md](proof-policy-mvp.md) |
| 보안/로직 점검 | [security-and-logic-review.md](security-and-logic-review.md) |
| GitHub Sprint 4 진입 기준 | [github-sprint4-entry.md](github-sprint4-entry.md) |
| 차단 범위/계정/탈퇴 UX | [control-account-design.md](control-account-design.md) |

## Decisions

| ID | 결정 | 이유 | 상태 |
| --- | --- | --- | --- |
| D-001 | 인터뷰/설문 없이 build-first로 진행 | 사용자가 명시했고, 첫 리스크는 구매 의향이 아니라 모바일 차단 가능성이다. | Accepted |
| D-002 | Android를 첫 runnable platform으로 선택 | 현재 환경에 Android SDK/JDK가 있고 Gradle Wrapper 빌드가 가능하다. Xcode 앱은 아직 없다. | Accepted |
| D-003 | Android MVP는 `UsageStatsManager + foreground service + overlay` | 선택 앱 감지와 차단 화면 검증에 충분하고, AccessibilityService보다 정책 리스크가 낮다. | Accepted |
| D-004 | Android MVP에서 AccessibilityService 제외 | Google Play 정책상 non-accessibility 목적 사용은 설명/승인 부담이 크고, 자율 UI 조작으로 보이면 위험하다. | Accepted |
| D-005 | Android installed-app 전체 스캔을 MVP에서 제외 | broad package visibility는 정책 부담이 있다. 초기에는 수동 package 입력과 최근 foreground package 표시로 충분하다. | Accepted |
| D-006 | iOS는 FamilyControls/ManagedSettings 기반 | 소비자 앱에서 선택 앱 shield에 가장 정직한 API 경로다. 전체 기기 잠금은 약속하지 않는다. | Accepted |
| D-007 | iOS UI는 selected app 이름 표시를 약속하지 않음 | FamilyActivitySelection은 opaque value를 사용하므로 privacy model에 맞춘다. | Accepted |
| D-008 | Sprint 1-3은 local-only `MobileCreditState` 유지 | API/GitHub 없이 모바일 enforcement를 먼저 검증해야 한다. | Accepted |
| D-009 | 서버 sync 이후 server state를 source of truth로 전환 | 여러 기기와 GitHub ledger가 들어오면 로컬 상태만으로 일관성을 유지할 수 없다. | Deferred |
| D-010 | GitHub scoring은 PR 중심, rules-first, ledger-first | commit 수 보상은 악용되기 쉽고, 설명 가능한 rule이 AI 단독 판정보다 신뢰를 만든다. | Deferred |
| D-011 | LLM은 full diff 심사자가 아니라 explanation 보조층 | privacy, 비용, 신뢰 리스크를 낮추고 rules-first 정책을 유지한다. | Deferred |
| D-012 | 금전 스테이크, 부모/학교, 삭제 방지는 MVP 제외 | 결제/미성년자/MDM/스토어 정책 리스크가 핵심 검증을 흐린다. | Accepted |
| D-013 | 제품 포지션을 generic blocker가 아니라 developer proof ledger로 고정 | 차단 앱 시장은 붐비고 무료/저가 대안이 많다. 수익 명분은 차단 자체가 아니라 검증 가능한 개발 활동 ledger와 설명 가능한 credit 정책에서 나온다. | Accepted |
| D-014 | Sprint 4 전 market/dogfood gate를 추가 | GitHub scoring을 본격 구현하기 전에 모바일 차단 효용, 본인 반복 사용, 자연스러운 scorable dev event 빈도를 확인해야 한다. | Accepted |
| D-015 | 사용자-facing 제품은 score가 아니라 ledger/minutes/reasons 중심으로 설계 | raw score를 전면 노출하면 점수 게임을 유도한다. 사용자는 minutes, proof tier, reasons, risk flags만 보면 된다. | Accepted |
| D-016 | 결제 구현은 Gate E 전까지 금지 | 단순 blocker 구독은 가격 저항이 크다. proof ledger가 blocker 없이도 가치 있음을 확인하기 전에는 결제 기능을 만들지 않는다. | Accepted |
| D-017 | Android는 별도 debug log를 제거하고 dogfood event log를 단일 source of truth로 사용 | 같은 이벤트를 두 저장소에 쓰면 UI/TSV/summary가 어긋난다. | Accepted |
| D-018 | GitHub webhook placeholder는 API에서 제거 | 실제 PR enrichment 없이 scoring decision을 반환하면 제품/테스트 신뢰를 깎는다. Sprint 4에서 dedupe/enrichment/ledger와 함께 다시 추가한다. | Accepted |
| D-019 | 브랜드 톤은 “개발자지만 난 괜찮아”처럼 재미있고 개발자스럽게 간다 | 일반 blocker로 보이면 차별화가 약하다. 단, 장난 UX는 보안/권한/데이터 삭제와 분리한다. | Accepted |
| D-020 | 수동 todo 완료만으로 unlock하지 않는다 | 제품 차별점은 self-report가 아니라 개발 증거 기반 credit이다. Todo는 quest label이고 proof-backed completion만 policy에 영향을 준다. | Accepted |
| D-021 | 요일/휴일/free day/emergency unlock은 credit보다 상위 정책으로 둔다 | 예외 정책이 없으면 사용자는 앱을 삭제하거나 권한을 꺼버린다. 단, 모든 예외는 event log와 ledger에 남긴다. | Accepted |
| D-022 | GitHub scoring 재개 전 MVP progress audit을 통과 기준으로 사용 | 현재 병목은 scoring 코드가 아니라 실기기 dogfood 데이터 부재다. Gate A/B/C 판단 없이 Sprint 4로 가면 잘못된 product risk를 늦게 발견한다. | Accepted |
| D-023 | 다음 4개 PR은 dogfood runbook, event store tests, dogfood review UX, GitHub Sprint 4 entry spec 순서로 진행 | 모바일 차단 가치와 데이터 품질을 확인한 뒤 proof ledger로 넘어가야 한다. | Accepted |
| D-024 | 디자인 방향은 developer utility dashboard + playful edge로 고정 | Opal/one sec/Freedom/Jomo는 차단 UX가 강하지만 generic blocker 시장이 붐빈다. 수익 명분은 GitHub/WakaTime류 개발자 proof ledger를 전면에 두는 데 있다. | Accepted |
| D-025 | API는 local-only/CORS-closed 기본값으로 둔다 | 현재 API는 health-only scaffold다. Sprint 4 전까지 외부 네트워크와 browser origin을 기본 허용하면 불필요한 공격면이 생긴다. | Accepted |
| D-026 | Gate D: Trust And Privacy를 GitHub sync 착수 조건으로 승격 | 제품의 핵심 리스크는 강제 차단보다 package/repo/diff/privacy 데이터 최소화다. webhook HMAC, dedupe, retention, revoke/delete가 없으면 Sprint 4를 시작하지 않는다. | Accepted |
| D-027 | `mvp-execution-plan.md`를 현재 실행 source of truth로 둔다 | 문서가 많아져 실행 순서가 분산됐다. 중복 문서를 삭제하기 전에 active/reference/archive 후보를 분리하고 PR 순서를 고정한다. | Accepted |
| D-028 | Android MVP-A는 14일 dogfood runbook으로 Gate A/B/C/D를 판단한다 | 기능 추가보다 실기기 반복 데이터가 먼저다. runbook 없이 GitHub scoring이나 UI 확장을 진행하면 제품 리스크를 잘못 읽을 가능성이 높다. | Accepted |
| D-029 | TypeScript와 Android 정책 엔진은 공통 golden fixture로 drift를 막는다 | 모바일 enforcement와 서버/공유 정책이 어긋나면 같은 사용자 상태에서 다른 차단 결과가 나온다. Sprint 4 전부터 공통 JSON fixture를 양쪽 테스트에 적용한다. | Accepted |
| D-030 | GitHub Sprint 4는 HMAC/dedupe/retention/ledger idempotency부터 시작한다 | PR enrichment 없는 scoring이나 dedupe 없는 ledger write는 제품 신뢰와 privacy를 동시에 깨뜨린다. GitHub runtime은 [github-sprint4-entry.md](github-sprint4-entry.md)의 PR A-F 순서를 따른다. | Accepted |
| D-031 | 앱은 삭제 가능하고, 선택한 target만 차단한다 | B2C self-control 제품에서 uninstall prevention, 전체 기기 잠금, 모든 서비스 차단은 정책/신뢰 리스크가 크다. 로그인/로그아웃/회원탈퇴/데이터 삭제는 [control-account-design.md](control-account-design.md)를 따른다. | Accepted |
| D-032 | 모든 신규 기능은 product/security hardening gate를 먼저 통과한다 | 기획, 보안, 개인정보, 플랫폼 정책이 따로 움직이면 수익성 없는 blocker나 과잉 수집 GitHub 앱이 된다. 신규 기능은 [product-security-hardening-plan.md](product-security-hardening-plan.md)의 invariants, gate, stop list를 먼저 만족해야 한다. | Accepted |
| D-033 | paid 제품은 generic blocker가 아니라 proof ledger + cross-device policy로 판다 | Opal/Freedom/Jomo/Roots/ScreenZen/Cold Turkey/FocusMe가 blocker 시장을 이미 차지하고, ScreenZen 같은 무료 대안이 가격 기준을 낮춘다. Commit-to-Unlock의 유료 명분은 [competitive-service-review.md](competitive-service-review.md)의 proof ledger, browser/desktop companion, multi-source proof, sync/history에 둔다. | Accepted |
| D-034 | 오래된 PRD/스프린트/스냅샷 문서는 삭제하고 active docs만 유지한다 | 같은 결정을 여러 문서가 다르게 설명하면 구현 순서가 흔들린다. 삭제된 문서의 유효한 내용은 [mvp-execution-plan.md](mvp-execution-plan.md), [app-design.md](app-design.md), [competitive-service-review.md](competitive-service-review.md), [product-security-hardening-plan.md](product-security-hardening-plan.md)에 흡수한다. | Accepted |
| D-035 | 현재 MVP는 code-complete가 아니라 dogfood-data-gated 상태다 | Android local prototype은 빌드/테스트 가능한 수준까지 왔지만, 실제 기기에서 14일 데이터가 없으면 제품성/수익성 판단은 아직 불가능하다. | Accepted |
| D-036 | Android emulator smoke는 통과했지만 real-device Gate A/D를 대체하지 않는다 | Android 13 AVD에서 0분 차단, +5분 허용, 60초 차감은 확인됐다. 하지만 제조사 배터리/overlay/foreground service 제약은 물리 기기에서만 드러난다. | Accepted |
| D-037 | 다음 보완 순서는 real-device smoke, monitor reliability, desktop/browser companion, webhook security foundation이다 | 부족한 것은 기능 아이디어가 아니라 evidence와 paid moat다. stale monitor state와 desktop/browser 확장을 먼저 다룬 뒤 GitHub runtime은 HMAC/dedupe부터 시작한다. | Accepted |
| D-038 | 남은 작업 계획은 새 문서가 아니라 `mvp-execution-plan.md`에 유지한다 | 문서가 다시 분산되면 실행 순서가 흔들린다. 부족한 이유는 `mvp-gap-analysis.md`, 실제 PR 순서는 `mvp-execution-plan.md`, 변경 불가 결정은 이 문서에 둔다. | Accepted |
| D-039 | Monitor 상태 표시는 저장된 bool이 아니라 heartbeat 기반 runtime evidence를 사용한다 | force-stop/reinstall/reboot에서 저장값만 보면 UI가 실제로 죽은 service를 running으로 표시할 수 있다. 사용자가 켜고 싶어 하는 상태와 실제 heartbeat 상태를 분리한다. | Accepted |

## Revisit Triggers

아래 조건이 생기면 결정을 다시 본다.

- Android UsageStats foreground 감지가 주요 기기에서 안정적으로 동작하지 않는다.
- Overlay가 제조사 OS에서 반복적으로 제한되어 차단 UX가 무의미하다.
- iOS Family Controls entitlement가 장기간 승인되지 않는다.
- GitHub private repo 연결 거부가 실제 사용에서 핵심 blocker가 된다.
- PR-only scoring이 개인 개발자 activation을 크게 떨어뜨린다.
- 14일 dogfood에서 monitor enabled day가 8일 미만이거나 override가 주 3회를 넘는다.
- 14일 동안 자연스럽게 생긴 scorable dev event가 5개 미만이다.
- 사용자가 제품을 “개발 보상 ledger”가 아니라 “비싼 차단 앱”으로 인식하는 신호가 강하다.
