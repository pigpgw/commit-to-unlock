# Competitive Service Review

문서 상태: v0.1
조사일: 2026-05-04
역할: 다른 스크린타임, 포커스, earn-to-unlock, 개발자 생산성 서비스를 비교하고 Commit-to-Unlock의 기획/수익화/개발 우선순위를 보완한다.

## 1. Executive Verdict

Commit-to-Unlock은 `earn screen time app`이라고만 말하면 약하다. 걸음, 운동, 퀴즈, 명상, QR/NFC, strict mode로 앱을 여는 제품은 이미 많다. 이 제품은 아래처럼 더 좁혀야 한다.

```text
Developer proof ledger first. Selected-app enforcement second.
```

차별점은 `앱을 막는다`가 아니다. 차별점은 GitHub/WakaTime/IDE/Git proof를 설명 가능한 leisure credit ledger로 바꾸고, 그 ledger가 사용자가 고른 방해 앱 접근권을 조절하는 것이다.

보완 결정:

- Android local blocker는 계속 MVP-A로 유지한다.
- Android-only blocker로 과금하지 않는다.
- paid moat는 GitHub/WakaTime proof history, cross-device sync, browser/desktop enforcement, 설명 가능한 ledger다.
- Jomo/Unpluq/one sec처럼 unlock friction은 참고하되, 우리 unlock proof는 자기신고나 카메라 이벤트가 아니라 개발 산출물이어야 한다.
- Freedom/FocusMe/Cold Turkey가 보여주듯 개발자 paid product에는 desktop/browser path가 중요하다. 모바일 dogfood 후 browser/desktop companion을 별도 spike로 준비한다.

## 2. Competitive Map

| 범주 | 대표 서비스 | 핵심 | 우리 판단 |
| --- | --- | --- | --- |
| Premium mobile blocker | Opal, Jomo, Roots | 앱/웹 차단, strict/deep focus, reports | 같은 축으로 경쟁 금지. UX와 pricing anchor만 참고 |
| Free/donation blocker | ScreenZen | 무료 app/site blocker | local blocker는 무료 기대치가 강함 |
| Cross-device blocker | Freedom, one sec, FocusMe | desktop/mobile/browser 동시 제어 | paid 제품에는 cross-device가 필요 |
| Desktop hard blocker | Cold Turkey, FocusMe | desktop app/site/system-level block, locking | 개발자 방해가 desktop에 많으므로 유력 확장 |
| Physical friction | Unpluq, Brick/Blok류 | NFC/QR/태그/장벽으로 unlock friction | hardware는 MVP 제외. emergency/barrier UX만 참고 |
| Earn-to-unlock | Strut, Walki, Earn Scroll, EarnIt, ScrollToll | 걸음/운동/학습으로 screen time 획득 | 카테고리 존재는 수요 증거. 하지만 "earn" 자체는 차별점 아님 |
| Developer stats/accountability | WakaTime, Beeminder Gitminder | coding stats, commit/PR stats, pledge | 개발 proof에 돈을 내는 시장 신호. 우리 차별점은 ledger + enforcement |

## 3. Service Teardown

### Opal

Opal은 premium iOS/Mac/Android blocker의 기준점이다. 공식 가격은 free plan, 연 $99.99, 월 $19.99, lifetime $399를 제시하고, hard blocking, recurring sessions, whitelist, focus score history를 Pro에 둔다. App Store 설명은 blocklist/allowlist, app limits, mindful block screens, focus report, leaderboard, Deep Focus를 전면에 둔다.

배울 점:

- 상태 수치와 report가 신뢰를 만든다.
- high-price blocker도 가능하지만 polish와 brand가 필요하다.
- focus history는 paid feature가 될 수 있다.

피할 점:

- leaderboard/social comparison은 개발자 self-regulation과 맞지 않는다.
- "Deep Focus처럼 절대 못 푼다"는 약속은 플랫폼/정책 리스크가 크다.
- Opal과 같은 generic wellness tone으로 가면 가격 비교에서 불리하다.

제품 반영:

- 우리는 `Focus Score` 대신 `Credit Ledger`와 `Proof Feed`를 전면에 둔다.
- paid history는 screen time history가 아니라 proof/ledger/scoring explanation history여야 한다.

### Freedom

Freedom은 cross-device blocker의 기준점이다. free plan도 apps/websites/entire internet block, unlimited devices, custom blocklists, desktop app blocking을 제공한다. Premium은 recurring/advanced scheduling, longer sessions, Locked Mode를 제공하고, 가격은 yearly $3.33/month, monthly $8.99, lifetime $99.50로 공개되어 있다.

배울 점:

- cross-device sync는 paid가 아니라 기본 기대치에 가깝다.
- desktop app/site blocking은 개발자에게 특히 중요하다.
- advanced scheduling과 locked mode는 유료 전환 명분이 된다.

피할 점:

- `entire internet` 포지션은 우리 MVP와 맞지 않는다.
- Locked Mode처럼 logout/설정 변경 제한을 강하게 걸면 account/delete UX와 충돌할 수 있다.

제품 반영:

- Commit-to-Unlock paid Pro는 모바일만으로는 약하다.
- Gate A/B 이후 `browser/desktop companion`을 Sprint 후보로 올린다.
- strictMode는 "삭제 방지"가 아니라 "테스트 shortcut/편의 override 제한"으로 유지한다.

### ScreenZen

ScreenZen은 donation-supported, free, multi-platform blocker를 전면에 둔다. 공식 사이트는 "Less screen time. No subscription"과 "iOS, macOS, Windows and Android"를 강조한다.

배울 점:

- 단순 blocker는 free baseline이 강하다.
- donation/free 제품이 존재하므로 Android local blocker는 과금 명분이 아니다.

제품 반영:

- Free Local은 계속 무료로 둔다.
- Local Plus를 팔더라도 one-time utility에 가깝게 설계한다.
- 구독은 proof processing, sync, history, multi-device enforcement에만 붙인다.

### one sec

one sec은 완전 차단보다 `pause/intervention`을 잘 만든 서비스다. 공식 사이트는 앱/웹 interruption, browser extension, Structured/Lengo integration, local intervention logic, private data stance, cross-device use를 강조한다.

배울 점:

- block screen은 혼내는 화면보다 "잠깐 멈추고 의식적으로 선택"하게 해야 한다.
- local/privacy message는 짧고 명확해야 한다.
- browser extension은 mobile blocker의 자연스러운 확장이다.
- 외부 workflow integration이 paid value가 된다.

제품 반영:

- Android overlay는 `왜 막혔는지`, `다음 unlock proof`, `emergency unlock`을 즉시 보여준다.
- GitHub/WakaTime/IDE integration은 one sec의 Structured/Lengo 같은 workflow integration 포지션으로 설명한다.

### Jomo

Jomo는 iPhone/Mac screen time product로 sessions, locks, limits, routines, strict mode, unlock actions, journaling, brickphone mode, Mac website/app blocking을 제공한다. 가격은 free, annual $29.99, single purchase $99.99가 공개되어 있다. 특징적으로 unlock에 wait, reason, painful text, password, QR, AI proof 같은 friction을 둔다.

배울 점:

- 사용자는 "완전 금지"보다 "해제하려면 이유/행동이 필요함"을 받아들인다.
- journaling과 reason log는 override를 shame 없이 기록하는 데 유용하다.
- Mac companion은 mobile blocker의 paid value를 강화한다.

피할 점:

- Jomo의 strict/unbreakable copy와 deletion prevention 계열은 우리 B2C 원칙과 맞지 않는다.
- AI proof/self-proof unlock은 조작 가능성이 있고 개발자 proof 차별점을 흐린다.

제품 반영:

- emergency unlock reason은 계속 유지하되, failure가 아니라 recorded exception으로 보여준다.
- quest는 self-check가 아니라 proof-backed completion만 인정한다.
- "AI proof" 대신 "GitHub/WakaTime/Git proof"로 간다.

### Roots

Roots는 iOS 중심 screen time app으로 digital dopamine, balance score, scroll replacements, monk mode, daily goals, streaks, cheat days를 제공한다. 공식 가격은 free, $59.99/year, $9.99/month다.

배울 점:

- score, trend, streak, goal은 retention 장치가 될 수 있다.
- speed bump와 playful unblock action은 사용자의 거부감을 낮춘다.

피할 점:

- dopamine/mental health/wellbeing 톤은 generic 시장으로 빨려 들어간다.
- score/streak가 제품의 중심이 되면 개발 proof ledger가 약해진다.

제품 반영:

- streak는 부가 UI로만 둔다.
- proof ledger와 weekly rhythm을 중심으로 둔다.

### Unpluq And Physical Friction

Unpluq은 physical/digital barriers가 강하다. 공식 FAQ는 free version에서 2 apps/1 schedule/2 barriers, premium에서 더 많은 schedules/apps/barriers를 제공한다고 설명한다. 공식 store page는 1년 subscription $66, iOS 최대 49 apps per schedule, Android unlimited apps, NFC Tag/digital barriers, emergency mode를 제공한다고 밝힌다.

배울 점:

- 순수 소프트웨어보다 물리적/의식적 마찰을 신뢰하는 사용자가 있다.
- emergency mode는 필요하지만 악용 방지를 위해 횟수/시간 제한이 필요하다.
- "앱을 삭제하지 않아도 유용한 것만 제한한다"는 메시지가 좋다.

피할 점:

- hardware는 제조/배송/support가 붙으므로 MVP 범위 밖이다.
- physical barrier와 subscription을 동시에 요구하면 early dev ICP에는 진입 장벽이 높다.

제품 반영:

- emergency unlock은 유지하고 cap을 둔다.
- physical device는 만들지 않는다.
- "선택한 방해 앱만 제한하고 essentials는 유지" 메시지를 계속 유지한다.

### Cold Turkey And FocusMe

Cold Turkey는 desktop hard blocker의 강한 reference다. free는 website block/timed blocks/statistics를 제공하고, Pro는 app blocking, scheduled blocks, locking features, application password, breaks/allowances를 제공한다. Features page는 locked block, random text, time range, restart/password, Frozen Turkey, local statistics export를 설명한다.

FocusMe는 Windows/Mac/Linux desktop blocker를 전면에 두고 system-level site/app/game blocking, enforced mode, screen breaks, high configurability, sync를 강조한다.

배울 점:

- 개발자/PC 사용자에게 desktop distraction blocker는 강한 value다.
- one-time desktop license와 lifetime model은 subscription resistance를 줄일 수 있다.
- allowance/break 모델은 credit ledger와 자연스럽게 맞는다.
- local statistics export와 privacy stance는 개발자에게 중요하다.

제품 반영:

- browser/desktop companion은 fallback이 아니라 paid moat 후보로 둔다.
- Android dogfood가 통과해도 desktop/browser spec은 별도 작성한다.
- Local Plus one-time은 Android보다 desktop/browser utility에 더 적합하다.

### Earn-To-Unlock Apps

Strut/Walki는 steps to unlock, Earn Scroll/ScrollToll/Repscroll은 exercise/AI pose detection to unlock, EarnIt/PrepScroll은 learning/quizzes to unlock을 제공한다.

배울 점:

- "screen time을 벌어서 쓴다"는 행동 모델은 이미 설명 가능하고 시장에 존재한다.
- daily wallet, reset at midnight, selected locked apps, spend earned minutes 모델은 직관적이다.
- proof가 machine-observed일수록 자기신고보다 설득력이 높다.

피할 점:

- 걸음/운동/학습과 직접 경쟁하면 개발자 특화가 사라진다.
- 카메라/HealthKit/학습 콘텐츠는 별도 privacy/content burden을 만든다.

제품 반영:

- `earn screen time`은 category language로만 쓰고, hero message는 `developer proof`로 좁힌다.
- daily wallet/reset/spend UX는 가져오되, earning source는 GitHub/WakaTime/Git proof로 유지한다.

### WakaTime And Beeminder

WakaTime은 coding activity dashboard다. free는 1 week dashboard history, Premium은 unlimited history, programming goals, commit & PR stats, integrations, export/download stats를 제공한다. yearly 기준 Premium은 $12.83/month로 공개되어 있다.

Beeminder Gitminder는 GitHub repository stats를 연결해 commits/issues goal을 추적하고, pledge를 어기면 실제 돈을 청구하는 model이다.

배울 점:

- 개발자는 coding stats/history/commit PR stats에 돈을 낼 수 있다.
- GitHub activity 기반 accountability는 이미 이해되는 행동 모델이다.
- export/download와 private/team dashboards는 paid feature가 될 수 있다.

피할 점:

- money pledge는 강하지만 결제/환불/미성년자/감정적 반발 리스크가 커서 MVP 제외가 맞다.
- WakaTime처럼 측정만 하고 끝나면 Commit-to-Unlock의 enforcement 차별점이 약해진다.

제품 반영:

- GitHub-only가 부족하면 WakaTime/IDE proof fallback을 Sprint 5가 아니라 Sprint 4 decision point로 올린다.
- paid Pro는 `proof history + ledger export + multi-source proof + policy sync`가 핵심이다.

## 4. Revised Product Strategy

기존:

```text
Ship code. Earn screen time.
```

보완:

```text
Verified dev work becomes guilt-free leisure credit.
```

이유:

- `earn screen time`은 이미 운동/학습 앱들이 쓰는 표현이다.
- `verified dev work`가 유일한 차별점이다.
- `leisure credit`은 일반 screen time보다 개발자 도구/장부 느낌이 강하다.

## 5. Roadmap Changes

### Immediate Queue

기존 immediate queue는 유지하되, 경쟁 조사 기준으로 이유를 보강한다.

1. `feature/android-target-guardrails`
   selected target만 막는 신뢰 약속을 코드로 고정한다.

2. `test/android-real-device-smoke`
   generic blocker처럼 보이기 전에 실제 enforcement가 되는지 확인한다.

3. `docs/desktop-browser-companion-spike`
   Freedom, one sec, Jomo, Cold Turkey, FocusMe가 보여준 cross-device/desktop paid moat를 우리 proof ledger와 연결하는 설계를 작성한다.

4. `feature/github-webhook-security`
   GitHub proof ledger의 첫 runtime PR. HMAC/dedupe/inbound event만 구현하고 ledger write는 아직 하지 않는다.

5. `docs/wakatime-ide-proof-spike`
   PR-only proof가 적을 때의 fallback을 설계한다. WakaTime/IDE는 capped provisional credit로만 시작한다.

### Gate Changes

| Gate | 보완 |
| --- | --- |
| Gate A | Android enforcement가 통과해도 desktop/browser companion을 paid moat 후보로 유지 |
| Gate B | local blocker를 매일 켜두지 않으면 generic blocker 실패로 보고 proof ledger/report 중심으로 축소 |
| Gate C | PR-only event가 적으면 Sprint 4 범위에 commit batch/WakaTime spike를 넣음 |
| Gate D | 경쟁 서비스처럼 privacy/local-first copy가 약하면 GitHub sync 금지 |
| Gate E | proof ledger가 blocker 없이도 볼 가치가 없으면 subscription 금지 |

## 6. Feature Decisions After Review

| 기능 | 결정 | 이유 |
| --- | --- | --- |
| Android selected package guardrail | build now | 선택 target만 차단한다는 신뢰 약속을 코드화 |
| App/session leaderboard | do not build | Opal/Roots/EarnIt류와 겹치고 privacy risk 큼 |
| Generic focus score | do not build | Opal/Roots와 직접 경쟁하게 됨 |
| Proof Feed | build after GitHub security foundation | WakaTime류 paid value와 연결 |
| Ledger export | build with account/privacy work | 개발자 신뢰와 paid value |
| Browser/desktop companion | write spike doc soon | Freedom/Jomo/FocusMe/Cold Turkey가 paid moat를 증명 |
| WakaTime/IDE proof | design fallback | PR-only activation risk 대응 |
| Hardware/NFC unlock | do not build | Unpluq/Brick류와 경쟁하면 제조/support 부담이 커짐 |
| Money stake | do not build | Beeminder류 수요는 있으나 법무/결제 리스크가 큼 |

## 7. Sources

- Opal pricing: https://opalapp.com/pricing
- Opal App Store: https://apps.apple.com/us/app/opal-screen-time-for-focus/id1497465230
- Freedom pricing: https://freedom.to/premium
- ScreenZen: https://screenzen.co/
- one sec: https://one-sec.app/
- Jomo pricing: https://jomo.so/pricing
- Jomo features: https://jomo.so/features
- Roots pricing: https://www.getroots.app/pricing
- Unpluq app store: https://www.unpluq.com/products/unpluq-subscription-only
- Unpluq FAQ: https://www.unpluq.com/faq
- Cold Turkey pricing: https://getcoldturkey.com/pricing/
- Cold Turkey features: https://getcoldturkey.com/features/
- FocusMe pricing: https://focusme.com/pricing/
- FocusMe features: https://focusme.com/how-it-works/
- WakaTime pricing: https://wakatime.com/pricing
- Beeminder Gitminder: https://www.beeminder.com/gitminder/
- Strut: https://www.strut-app.com/
- Earn Scroll: https://earnscroll.app/
- EarnIt: https://www.earn-it.uk/
- ScrollToll: https://scrolltoll.com/
- Repscroll: https://repscroll.com/
