# Commit-to-Unlock Decision Log

문서 상태: v0.3
목적: 구현자가 다시 판단하지 않아도 되는 제품/기술 결정 기록

## Current Source Of Truth

| 주제 | 기준 문서 |
| --- | --- |
| 전체 제품/기술 설계 | [app-design.md](app-design.md) |
| proof/quest/policy MVP | [proof-policy-mvp.md](proof-policy-mvp.md) |
| 제품 전략/UX/사업 패키징 | [product-strategy-spec.md](product-strategy-spec.md) |
| 실행 순서 | [build-first-execution-plan.md](build-first-execution-plan.md) |
| 시장/니즈/피벗 게이트 | [market-needs-and-pivot-plan.md](market-needs-and-pivot-plan.md) |
| Android Sprint 1.1 | [android-sprint-1.1-design.md](android-sprint-1.1-design.md) |
| 모바일 credit contract | [mobile-credit-contract.md](mobile-credit-contract.md) |
| GitHub MVP PRD | [mvp-prd.md](mvp-prd.md) |
| 저장소 정리/삭제 기준 | [repository-audit-and-cleanup.md](repository-audit-and-cleanup.md) |

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
| D-016 | 결제 구현은 Gate 4 전까지 금지 | 단순 blocker 구독은 가격 저항이 크다. proof ledger가 blocker 없이도 가치 있음을 확인하기 전에는 결제 기능을 만들지 않는다. | Accepted |
| D-017 | Android는 별도 debug log를 제거하고 dogfood event log를 단일 source of truth로 사용 | 같은 이벤트를 두 저장소에 쓰면 UI/TSV/summary가 어긋난다. | Accepted |
| D-018 | GitHub webhook placeholder는 API에서 제거 | 실제 PR enrichment 없이 scoring decision을 반환하면 제품/테스트 신뢰를 깎는다. Sprint 4에서 dedupe/enrichment/ledger와 함께 다시 추가한다. | Accepted |
| D-019 | 브랜드 톤은 “개발자지만 난 괜찮아”처럼 재미있고 개발자스럽게 간다 | 일반 blocker로 보이면 차별화가 약하다. 단, 장난 UX는 보안/권한/데이터 삭제와 분리한다. | Accepted |
| D-020 | 수동 todo 완료만으로 unlock하지 않는다 | 제품 차별점은 self-report가 아니라 개발 증거 기반 credit이다. Todo는 quest label이고 proof-backed completion만 policy에 영향을 준다. | Accepted |
| D-021 | 요일/휴일/free day/emergency unlock은 credit보다 상위 정책으로 둔다 | 예외 정책이 없으면 사용자는 앱을 삭제하거나 권한을 꺼버린다. 단, 모든 예외는 event log와 ledger에 남긴다. | Accepted |

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
