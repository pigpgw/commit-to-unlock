# Design Research And UX Direction

문서 상태: v0.1
조사일: 2026-05-03
역할: 인터넷 기반 디자인 조사, UX 방향, Android/iOS 화면 보완 기준

## 1. Decision

`개발자지만 난 괜찮아`는 generic wellness blocker처럼 보이면 안 된다. 최종 디자인 방향은 `developer proof ledger + selected-app enforcement`다.

즉 첫 화면과 차단 화면은 재미있고 개발자스럽게 가되, 홈/권한/정책/장부 화면은 GitHub/WakaTime 같은 개발자 도구의 밀도와 신뢰감을 따라간다.

디자인 판단:

- 화면은 marketing hero가 아니라 바로 쓸 수 있는 tool surface로 시작한다.
- 색과 컴포넌트는 GitHub Primer 계열의 neutral, status color, dense list를 기본으로 한다.
- Android는 Material 3 컴포넌트와 플랫폼 권한 흐름을 따른다.
- iOS는 Apple HIG와 Screen Time API의 privacy-preserving token 모델을 따른다.
- 장난은 onboarding, empty state, block copy에만 둔다.
- 권한, 데이터, 삭제, override, privacy copy는 절대 장난스럽게 쓰지 않는다.

## 2. Research Sources

| 범주 | 본 레퍼런스 | 확인한 포인트 | 적용 판단 |
| --- | --- | --- | --- |
| Strong blocker | Opal | focus blocks, blocklist, app limits, reports, app lock, mindful block screen | 차단 강도와 리포트는 참고하되 leaderboard/reward 과시는 MVP에서 제외 |
| Intervention | one sec | 앱/웹 접근 전 pause/intervention, strict block, local-first privacy messaging | 차단 전후에 짧은 의도 확인과 local-first 설명을 참고 |
| Cross-device blocker | Freedom | block apps/sites/internet, scheduling, locked mode, blocklists | schedule/blocklist UX는 참고. 전체 인터넷 차단 포지션은 피함 |
| Modern iOS blocker | Jomo | unlocks, strict mode, journaling, safe/unlock copy | strict/gentle 선택과 "you stay in control" copy를 참고 |
| Developer metrics | WakaTime | project/file/branch/commit/language metrics dashboard | Proof Feed와 Ledger는 metrics dashboard처럼 설계 |
| Developer UI system | GitHub Primer | GitHub의 color, typography, UI pattern foundation | 색/상태/리스트/타임라인의 기본 언어로 채택 |
| Platform design | Android Material 3 | Compose Material 3 components, type scale, Android app quality | Android prototype UI 개선 기준 |
| Platform design | Apple HIG | color consistency, typography legibility, icons, iOS characteristics | iOS prototype UI 기준 |

## 3. Competitive Design Findings

### Opal

Opal은 강한 앱 차단, blocklist, app limit, focus report, app lock을 전면에 둔다. 제품 디자인은 "screen time을 줄였다"는 수치와 block/session 설정을 중심으로 신뢰를 만든다.

가져올 것:

- 오늘 상태를 수치로 즉시 보여준다.
- blocklist와 schedule을 사용자가 직접 통제한다.
- block screen은 차단 사유와 다음 행동을 보여준다.

피할 것:

- 일반 wellbeing/ADHD/mental health 앱처럼 넓게 포지셔닝하지 않는다.
- leaderboard와 social comparison은 MVP에서 제외한다.

### one sec

one sec은 완전 차단보다 "열기 전에 멈추게 하는 개입"이 강점이다. 공식 페이지는 intervention logic이 device-local로 동작하고 data selling이 없다는 점도 강조한다.

가져올 것:

- 차단 화면은 혼내는 화면이 아니라 멈추고 판단하게 하는 화면이어야 한다.
- privacy/local-first 메시지는 짧고 구체적으로 둔다.
- 긴급 해제는 실패가 아니라 의식적 선택으로 기록한다.

피할 것:

- 호흡/명상/웰빙 톤을 그대로 가져오지 않는다. 이 제품은 개발자 proof 제품이다.

### Freedom

Freedom은 blocklists, recurring schedule, locked mode가 강하다. 사용자는 차단 대상과 시간표를 먼저 만든다.

가져올 것:

- Policy 화면은 blocked targets와 active schedule을 한 곳에서 편집한다.
- "locked/strict"는 사용자가 선택한 정책 강도임을 명확히 한다.

피할 것:

- "entire Internet" 같은 약속은 하지 않는다.
- Android-only MVP에서 cross-device blocker처럼 보이게 만들지 않는다.

### Jomo

Jomo는 strict/gentle block strength, unlock, journaling, "control remains with the user" 메시지가 강하다. blocking이 앱 삭제가 아니며 필요하면 unlock할 수 있다는 안전 copy도 좋다.

가져올 것:

- strictMode는 하나의 policy strength로 보여준다.
- override는 숨기지 말고 기록되는 예외로 보여준다.
- "앱이 삭제되는 게 아니라 선택한 접근만 제한된다"를 권한 화면에서 명시한다.

피할 것:

- succulent, joy/wellbeing 감성처럼 일반 소비자형 장식을 그대로 복제하지 않는다.

### WakaTime

WakaTime은 개발 활동을 프로젝트, 파일, 브랜치, 커밋, 언어 단위 metric으로 보여준다. 이 제품의 Proof Feed와 Ledger는 WakaTime의 "developer metrics" 문법을 따라야 한다.

가져올 것:

- PR/commit/review/CI event를 compact list로 보여준다.
- repo, branch, reason code, confidence, minutes를 명확한 열/행 구조로 둔다.
- "오늘 내가 왜 25분을 벌었는가"를 장부처럼 설명한다.

피할 것:

- 코드 시간을 그대로 productivity score로 만들지 않는다. 목표는 time tracking이 아니라 unlock ledger다.

### GitHub Primer

GitHub Primer는 developer-facing UI에 필요한 neutral palette, status color, typography, component pattern의 기준으로 적합하다.

가져올 것:

- 배경은 밝고 차분하게, 상태 색은 의미별로만 쓴다.
- list, timeline, label, badge, border를 활용한다.
- package name, repo, branch, reason code는 monospace로 보여준다.

피할 것:

- 검은 terminal UI와 neon hacker aesthetic에 치우치지 않는다.

## 4. Product Personality

브랜드 이름은 `개발자지만 난 괜찮아`를 유지한다. 영어 fallback은 `Commit-to-Unlock`이다.

톤 원칙:

| 상황 | 톤 | 예시 |
| --- | --- | --- |
| 첫 진입 | playful gate | `개발자이신가요?` |
| 개발자 yes | 약속 확인 | `좋습니다. 쉬는 시간은 커밋으로 정산합니다.` |
| 개발자 no | 장난스러운 종료 | `403: 개발자 인증 실패. 저리가. 여긴 PR로 문을 여는 곳입니다.` |
| 권한 안내 | 정확하고 건조함 | `Usage Access가 없으면 foreground app을 감지할 수 없습니다.` |
| 차단 | 유머 + 다음 행동 | `credit_empty: 아직 쉴 시간이 없습니다. 작은 PR이나 테스트 추가 후 다시 오세요.` |
| override | no shame | `긴급 해제는 실패가 아니라 예외입니다. 기록은 남습니다.` |
| privacy | 진지함 | `private repo raw diff는 기본 저장하지 않습니다.` |

금지 톤:

- 사용자를 모욕하는 문구
- 중독, ADHD, 실패를 조롱하는 문구
- "삭제 불가", "완전 통제", "AI가 폰을 지배" 같은 과장 문구
- 이유 없이 일본식/게임식 밈을 과하게 넣는 것

## 5. Visual System

### Palette

GitHub Primer에 가까운 light-first palette를 기본으로 한다.

| Token | Color | 용도 |
| --- | --- | --- |
| `background` | `#F6F8FA` | 앱 전체 배경 |
| `surface` | `#FFFFFF` | 주요 패널 |
| `surfaceMuted` | `#F0F3F6` | 보조 패널, disabled block |
| `textPrimary` | `#1F2328` | 본문/제목 |
| `textMuted` | `#636C76` | 보조 설명 |
| `border` | `#D0D7DE` | 구분선 |
| `accent` | `#0969DA` | primary action |
| `success` | `#1A7F37` | 허용, credit earned, pass |
| `warning` | `#9A6700` | needs_data, permission warning |
| `danger` | `#CF222E` | blocked, fail, risky action |
| `playful` | `#8250DF` | developer gate/empty state의 작은 장난 |

주의:

- 화면 전체를 purple/blue gradient로 덮지 않는다.
- dark hacker theme를 기본으로 하지 않는다.
- beige wellness app처럼 보이지 않게 한다.
- 장난 색은 포인트로만 쓰고 상태 색의 의미를 흐리지 않는다.

### Type

Android:

- system sans + Material 3 type scale
- package name, policy reason, repo ref는 monospace fallback

iOS:

- SF system font
- SF Symbols 기반 icon
- Dynamic Type 대응을 전제

공통:

- 영문 reason code는 monospace.
- 한국어 설명은 짧고 직접적으로.
- 긴 문단보다 상태 row와 reason list를 사용한다.

### Layout

원칙:

- card-in-card를 만들지 않는다.
- 중요한 상태는 상단 compact status panel에 둔다.
- 설정성 화면은 full-width section + rows로 구성한다.
- radius는 최대 8dp.
- 버튼은 command일 때만 text button을 쓰고, 반복 조작은 icon + label 또는 compact segmented control로 둔다.

권장 section:

- Status
- Permissions
- Targets And Policy
- Proof / Quest
- Emergency
- Dogfood Export

## 6. Information Architecture

### Android Prototype IA

| 화면/섹션 | 목적 | 우선순위 |
| --- | --- | --- |
| Developer Gate | 브랜드 톤 설정 | 유지 |
| Today Status | credit, monitor, policy reason, foreground package | 최우선 개선 |
| Permission Checklist | Usage Access, Overlay, Notification | 최우선 개선 |
| Targets And Policy | blocked package, strictMode, weekday/time/manual holiday | 유지/정리 |
| Quest And Proof | mock proof, required quest, free day | 유지 |
| Emergency | override duration/reason | 유지 |
| Dogfood Review | 14일 summary, event log, export | 다음 UI 개선 |

현재 Android UI는 기능이 많아졌으므로 다음 UI PR에서는 기능을 삭제하지 말고 section 정리와 copy 압축을 한다.

### MVP IA

| Tab | 목적 | 주요 컴포넌트 |
| --- | --- | --- |
| Today | 지금 열 수 있는가 | credit ring/bar, policy state, next unlock action |
| Proof Feed | 왜 시간이 생겼는가 | PR/commit/review timeline, reasons, minutes |
| Ledger | 장부/감사 | earned/spent/override/clawback rows |
| Targets | 무엇을 막는가 | selected apps/sites, strictness, active schedule |
| Privacy | 무엇을 저장하는가 | connected repos, retention, revoke/delete |

## 7. Block Overlay UX

Block overlay는 제품의 핵심 순간이다. 반드시 다음 정보를 포함한다.

- target package 또는 opaque target summary
- current policy reason
- remaining credit
- next unlock action
- emergency unlock path
- app으로 돌아가는 버튼

권장 copy:

```text
Blocked
com.youtube.android

credit_empty
오늘 남은 leisure credit이 없습니다.

작은 PR, 테스트 추가, 또는 mock proof로 credit을 만드세요.
긴급 해제는 기록됩니다.
```

strictMode가 false일 때만 test credit shortcut을 보여준다. strictMode가 true일 때는 shortcut을 숨기고, 왜 숨겼는지 한 줄로 설명한다.

## 8. Proof Ledger UX

사용자에게 raw score를 크게 보여주지 않는다. Ledger row는 다음 순서로 보여준다.

```text
+25 min  PR #42 merged
repo: side-project/app
reason: tests_added, ci_passed, review_activity
confidence: high
```

위험/감점 row:

```text
0 min  Commit batch ignored
reason: lockfile_only, duplicate_patch_risk
```

Override row:

```text
-10 min  Emergency unlock
reason: bank_auth
expires: 22:10
```

## 9. Design Risks

| 리스크 | 대응 |
| --- | --- |
| 재미있는 톤이 보안/권한 신뢰를 해침 | 장난은 gate/block/empty state로 제한 |
| generic blocker처럼 보임 | Proof Feed와 Ledger를 1차 제품 표면으로 노출 |
| 너무 많은 설정이 한 화면에 쌓임 | section, status chip, compact rows로 정리 |
| 개발자만 이해하는 밈으로 좁아짐 | 핵심 행동은 plain Korean/English로 설명 |
| 차단이 억울하게 느껴짐 | reason, next action, override를 항상 표시 |
| 과장된 strict mode로 정책 리스크 발생 | strictMode 의미를 UI에 제한적으로 명시 |

## 10. Next Design PRs

1. Android UI sectioning
   `MainActivity`의 현재 긴 제어 화면을 Status, Permissions, Targets, Quest, Emergency, Dogfood section으로 나눈다.

2. Android overlay copy polish
   `PolicyDecision.reason`별 사용자-facing copy map을 만들고, block overlay에 next action을 표시한다.

3. Dogfood review surface
   analyzer 결과의 Data Quality/Gate snapshot을 앱 안에서 최소 요약한다.

4. iOS prototype wireframe
   FamilyActivityPicker, shield toggle, opaque target count, local credit state를 기준으로 SwiftUI 화면 skeleton을 확정한다.

5. Brand asset pass
   앱 아이콘은 simple symbol 중심으로 만들고, Apple/Android platform icon 규칙을 따른다. GitHub 로고나 SF Symbols를 trademark처럼 쓰지 않는다.

## 11. Sources

- Opal official site: https://opalapp.com/
- Opal App Store listing: https://apps.apple.com/us/app/opal-screen-time-control/id1497465230
- one sec official site: https://one-sec.app/
- Freedom official site: https://freedom.to/
- Jomo official site: https://jomo.so/
- WakaTime metrics: https://wakatime.com/metrics-for-programming
- GitHub Primer design system: https://primer.github.io/design/
- Android Compose Material 3: https://developer.android.com/develop/ui/compose/designsystems/material3
- Android Material 3 release package: https://developer.android.com/jetpack/androidx/releases/compose-material3
- Apple Human Interface Guidelines: https://developer.apple.com/design/human-interface-guidelines/
- Apple HIG Color: https://developer.apple.com/design/human-interface-guidelines/color
- Apple HIG Typography: https://developer.apple.com/design/human-interface-guidelines/typography
- Apple HIG Icons: https://developer.apple.com/design/human-interface-guidelines/icons
