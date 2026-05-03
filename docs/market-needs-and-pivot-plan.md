# Commit-to-Unlock Market Needs And Pivot Plan

문서 상태: v0.2
조사일: 2026-05-03
범위: 인터뷰/설문 없이 공개 자료, 경쟁 서비스, 앱스토어/커뮤니티 신호를 바탕으로 한 기획 보강
역할: build-first 개발이 “만드는 재미”로만 흐르지 않도록 시장/니즈/피벗 게이트를 둔다.

## 1. 결론

Commit-to-Unlock은 계속 만들 가치가 있다. 단, 일반적인 “스크린타임 차단 앱”으로 만들면 수익성이 약하다. Opal, Freedom, Jomo, Roots, ScreenZen, one sec, Cold Turkey, FocusMe 같은 제품이 이미 강하고, 무료/저가 대안도 많다.

더 중요한 변화는 `earn screen time` 자체도 더 이상 완전히 새로운 포지션이 아니라는 점이다. 2026년 현재 공개적으로 보이는 앱만 봐도 걸음 수로 여는 Strut/Walki, 운동으로 여는 Earn Scroll, 학습으로 여는 EarnIt, 명상/호흡/펫 액션으로 여는 Roots류가 있다. 따라서 우리 차별점은 “무언가를 하면 앱을 열어준다”가 아니라 아래로 좁혀야 한다.

> Verified developer proof-of-work turns into explainable leisure credits.

즉, 제품은 `developer proof ledger + optional blocking`이어야 한다. 차단은 가치 전달 장치이고, 진짜 자산은 개발 활동을 검증 가능한 credit ledger로 바꾸는 규칙 엔진이다.

## 2. 조사 근거 요약

### 수요는 있다

- DataReportal Digital 2026은 온라인 성인이 social/video feed에 주당 평균 18시간 36분을 쓴다고 보고한다. 방해 앱 사용을 줄이고 싶어 하는 기본 수요는 크다.
- GitHub Octoverse 2025는 1억 8천만 명 이상의 개발자, 연 3,600만 명 이상의 신규 유입, 월평균 4,320만 merged PR을 보고한다. 개발 활동을 보상 신호로 쓸 공급 데이터도 충분하다.
- Stack Overflow 2025 조사 발표는 GitHub가 개발자가 쓰거나 쓸 예정인 주요 커뮤니티 플랫폼 중 67%, 코드 문서/협업 도구 중 81%라고 말한다. GitHub-first는 여전히 합리적이다.

### 하지만 일반 차단 앱 시장은 붐빈다

| 범주 | 예시 | 공개 가격/포지션 | 시사점 |
| --- | --- | --- | --- |
| 프리미엄 iOS 차단 | Opal, Roots | Opal Pro $99.99/year, lifetime $399, student discount | 강한 브랜드와 polished UX가 이미 있다. 같은 축으로 경쟁하면 불리하다. |
| 크로스디바이스 차단 | Freedom | yearly $3.33/mo, monthly $8.99/mo, lifetime $99.50 | 개발자에게 desktop/mobile 동시 차단은 매력적이다. 우리도 장기적으로 desktop/browser fallback이 필요하다. |
| 무료/기부형 | ScreenZen, TapBlok | ScreenZen free/donation, TapBlok free/open-source Android | 단순 local blocker는 가격 저항이 크다. 구독 명분이 약하다. |
| 행동 마찰 | one sec, Jomo | one sec 14.99 EUR/year, Jomo $29.99/year | “차단 전 한 번 멈추게 하기” 수요가 있다. block screen도 설명/반성 중심이어야 한다. |
| 물리적 마찰 | Brick, Unpluq, TapBlok | NFC/QR/태그 기반 | 사용자는 순수 소프트웨어보다 물리적 우회 비용을 신뢰하는 경우가 있다. |
| 행동 보상형 unlock | Strut, Earn Scroll, EarnIt, Roots | 걸음/운동/학습/호흡으로 unlock | earn-to-unlock은 검증된 니즈지만, 이미 카테고리가 생기고 있다. |
| 개발자 시간/성과 추적 | WakaTime, RescueTime | WakaTime Premium $14/mo, yearly $12.83/mo, commit/PR stats 포함 | 개발자는 개발 데이터 기반 productivity에는 돈을 낼 수 있다. blocker보다 ledger/insight가 과금 명분이다. |
| 계약/벌금형 습관 | Beeminder | GitHub commits/issues 목표와 pledge model | 강한 동기부여 수요는 있으나 결제/분쟁/미성년자 리스크가 커서 MVP 제외가 맞다. |

2026-05-03 재확인 결과도 같은 결론이다. Opal/Freedom은 이미 높은 가격의 paid focus system을 팔고, ScreenZen은 무료 기대치를 만든다. WakaTime은 developer stats에 월 구독을 붙이고, Beeminder는 GitHub commits/issues를 돈이 걸린 commitment로 연결한다. 따라서 Commit-to-Unlock이 돈을 받을 수 있는 축은 `local blocker`가 아니라 `developer proof ledger + cross-device policy + 설명 가능한 credit history`다.

### 공개 사용자 신호

앱스토어와 Reddit/Hacker News류 공개 글은 대표 표본이 아니다. 그래도 반복되는 니즈는 보인다.

- 사용자는 Apple Screen Time 같은 기본 제한을 너무 쉽게 우회한다고 느낀다.
- Opal/Freedom류에 돈을 내는 사용자는 “진짜 막힘”, “hard mode”, “cross-device”에 비용을 지불한다.
- 반대로 단순 local blocker 구독에는 가격 저항이 크다. 무료 대안이 많기 때문이다.
- 차단만으로는 충동을 해결하지 못한다는 불만이 반복된다. 사용자는 block screen에서 이유, 대안 행동, 의도 확인 같은 마찰을 원한다.
- earn-to-unlock에는 양면 반응이 있다. 일부는 “동기부여가 된다”고 보고, 일부는 “삶에서 이미 많은 걸 벌어야 하는데 스크린타임까지 벌고 싶지 않다”고 반응한다.

이 신호를 제품으로 번역하면, Commit-to-Unlock은 사용자를 벌주는 앱처럼 보이면 안 된다. “개발했으니 떳떳하게 쉰다”는 guilt-free reward language가 더 낫다.

## 3. ICP 재정의

### 1차 ICP: self-control developer

특성:

- GitHub, IDE, 터미널을 매일 쓴다.
- SNS/YouTube/Reddit/게임을 줄이고 싶다.
- 일반 blocker는 써봤거나 쉽게 우회했다.
- 자기신고형 todo보다 자동 검증을 선호한다.

왜 먼저인가:

- GitHub/WakaTime/IDE activity 같은 객관 신호를 이해한다.
- 개인정보/권한 설명을 읽을 가능성이 높다.
- “내가 만든 코드가 leisure credit이 된다”는 메시지가 직관적이다.

### 2차 ICP: coding student / portfolio builder

특성:

- 부트캠프, 대학, 취준, 사이드프로젝트를 한다.
- 매일 merged PR이 나오지는 않는다.
- commit batch, WakaTime, issue/task evidence가 필요하다.

주의:

- GitHub PR-only는 activation이 느릴 수 있다.
- 학생 가격과 low-friction proof channel이 필요하다.

### 3차 ICP: focus-app power user

특성:

- Opal, ScreenZen, one sec, Freedom, Cold Turkey 등을 이미 써봤다.
- 일반 blocker의 약점과 우회 루프를 안다.
- 개발자라면 Commit-to-Unlock의 차별점을 가장 빨리 이해한다.

### 뒤로 미룰 ICP

- 부모/학교/미성년자: 규제/동의/MDM 리스크가 크다.
- 기업/엔터프라이즈: 보안심사와 세일즈 주기가 길다.
- 비개발자 일반 사용자: earn-to-unlock 경쟁이 이미 많고 차별점이 약하다.

## 4. 제품 보완 결정

### 4.1 GitHub-only는 최종 포지션이 아니라 첫 high-trust proof channel이다

GitHub PR은 신뢰도는 높지만 빈도가 낮다. 특히 개인 학습자와 취준생은 매일 merged PR을 만들지 않는다. 따라서 proof channel을 단계적으로 넓힌다.

| Proof channel | 신뢰도 | 권장 credit 성격 | 구현 시점 |
| --- | --- | --- | --- |
| Merged GitHub PR + CI/review | 높음 | confirmed credit | Sprint 4 |
| GitHub commit batch | 중간 | provisional credit | Sprint 4 |
| WakaTime/IDE coding time | 중간 | capped provisional credit | Sprint 5 후보 |
| Local desktop focus session | 낮음~중간 | low capped credit | mobile enforcement 실패 시 pivot |
| Manual task/checklist | 낮음 | MVP 제외 또는 admin-only | 후순위 |

### 4.2 모바일 차단은 유지하되 desktop/browser fallback을 제품 설계에 넣는다

개발자의 방해는 모바일만이 아니다. YouTube, Reddit, X, Discord, 게임, 뉴스는 desktop에서도 크다. Android/iOS enforcement가 약하거나 심사 리스크가 높으면 제품을 죽이지 말고 아래로 전환한다.

Pivot A:

> Commit-to-Unlock Browser/Desktop: GitHub/WakaTime credit이 있어야 browser distractions가 열린다.

이 경로는 모바일 심사 리스크가 낮고, 개발자 업무 환경에 더 가깝다.

### 4.3 가격은 blocker가 아니라 ledger/insight에 붙인다

가격 저항을 고려하면 “그냥 앱 막아주는 기능”으로 월 $10을 받는 것은 위험하다. 권장 가격은 다음이다.

| 플랜 | 가격 가설 | 과금 명분 |
| --- | --- | --- |
| Free local | 무료 | Android local blocker, mock/local credit, 제한된 target |
| Local Plus | $29-49 one-time | 오프라인 local blocker 고급 기능. 구독 거부층 대응 |
| Pro | $4.99-6.99/mo 또는 $39-59/year | GitHub/WakaTime proof ledger, scoring explanation, cross-device sync |
| Student | $19-29/year | 학생/취준생용 낮은 진입가 |
| Cohort | $2-4/seat/mo | 부트캠프/코호트 리포트. leaderboard 제외 |

금전 stake는 여전히 제외한다. 법무/환불/미성년자/스토어 정책 리스크가 제품 검증보다 크다.

### 4.4 카피를 처벌형에서 회복형으로 바꾼다

나쁜 카피:

- “코딩 안 하면 앱 못 씀”
- “AI가 널 통제함”
- “폰 중독을 벌금으로 고침”

권장 카피:

- “Ship code. Earn guilt-free screen time.”
- “Verified work becomes leisure credits.”
- “Your block screen explains what to do next.”

한국어:

- “코드를 냈으면, 쉬는 시간도 떳떳하게.”
- “검증된 개발 활동을 방해 앱 크레딧으로 바꿉니다.”
- “막는 이유와 다음 unlock 조건을 설명합니다.”

## 5. No-Interview 검증 계획

사용자 인터뷰와 설문은 하지 않는다. 대신 행동 데이터와 공개 자료로 판단한다.

### Gate A: Enforcement viability

기존 local enforcement gate를 Gate A로 통합한다.

통과:

- Android에서 선택 앱 foreground 감지와 overlay가 실제 기기에서 동작한다.
- credit 0/credit > 0 상태 전환이 2초 안에 반영된다.
- 권한 회수/서비스 중지 원인이 UI와 dogfood event log에 드러난다.

실패 시:

- Android-only local blocker로 축소하거나 browser/desktop blocker로 전환한다.

### Gate B: Dogfood need

본인 실제 사용 14일 기준으로 판단한다.

통과:

- 14일 중 8일 이상 monitor를 켜 둔다.
- 주당 4회 이상 실제 blocked attempt가 발생한다.
- override가 주 3회 이하이다.
- 앱을 끄거나 삭제하고 싶은 순간이 있더라도, 다음 날 다시 켤 만큼 효용이 있다.

실패 시:

- “모바일 차단” 자체가 본인 문제와 맞지 않을 수 있다. desktop/browser-first로 전환한다.

### Gate C: Developer proof supply

GitHub scoring을 붙이기 전 14일 동안 본인 활동 기준으로 판단한다.

통과:

- 14일 동안 5개 이상 scorable dev events가 자연스럽게 생긴다.
- PR-only가 3개 미만이면 commit batch 또는 WakaTime channel을 Sprint 4 범위에 넣는다.
- credit을 얻으려고 의미 없는 commit을 만들고 싶은 유혹이 크면 scoring 규칙을 더 엄격하게 한다.

실패 시:

- GitHub PR-only는 너무 좁다. `GitHub + WakaTime/IDE`로 피벗한다.

### Gate D: Trust and privacy

통과:

- public repo mode 또는 metadata-only mode로 첫 경험을 만들 수 있다.
- private diff를 저장하지 않는 정책이 제품 UI에 명확하다.

실패 시:

- private repo 분석을 뒤로 미루고 local/IDE/WakaTime 기반으로 전환한다.

### Gate E: Monetization readiness

초기에는 결제/fake-door를 만들지 않는다. 아래 조건 전에는 과금 구현을 하지 않는다.

통과 조건:

- 14일 dogfood에서 스스로 계속 켜 둔다.
- proof ledger가 “차단 앱” 없이도 볼 가치가 있다.
- 최소 하나의 paid feature가 명확하다. 예: cross-device sync, GitHub/WakaTime scoring history, desktop/browser enforcement.

실패 시:

- 구독형 앱이 아니라 오픈소스/local utility 또는 one-time paid Android tool로 축소한다.

## 6. Pivot Map

| 실패 신호 | 해석 | 전환안 |
| --- | --- | --- |
| Android/iOS 차단이 불안정 | 모바일 OS 제약이 제품 가치를 깎는다 | desktop/browser blocker first |
| GitHub PR 이벤트가 너무 적다 | PR-only는 일상 reward loop가 느리다 | WakaTime/IDE/commit batch 추가 |
| 사용자가 “스크린타임을 벌기 싫다”고 느낀다 | 보상 언어가 처벌처럼 들린다 | blocker보다 proof ledger/report 중심 |
| 가격 저항이 크다 | generic blocker 구독으로 인식된다 | local one-time + cloud scoring Pro |
| AI 판정을 못 믿는다 | 개발자는 black-box 평가에 민감하다 | rules-first, no full-diff storage, appeal |
| 일반 사용자만 관심을 보인다 | dev-specific 차별점이 약하다 | 학습/운동 earn-to-unlock과 경쟁하지 말고 dev ICP로 재좁힘 |

## 7. 실행 계획 반영

현재 Sprint 1 Android local blocking은 그대로 진행한다. 단, Sprint 4 GitHub scoring 재개 전에 Gate B/C를 추가한다.

수정된 순서:

1. Android 실기기 차단 hardening
2. 14일 dogfood logging
3. iOS Xcode/entitlement 준비
4. 경쟁 앱 직접 teardown: Opal, ScreenZen, one sec, Jomo, WakaTime
5. Gate A/B/C 통과 여부 판단
6. 통과하면 GitHub scoring Sprint 4 진행
7. 실패하면 desktop/browser 또는 WakaTime/IDE proof channel로 전환

## 8. Sources

- Opal App Store: https://apps.apple.com/us/app/opal-screen-time-control/id1497465230
- Opal pricing: https://opalapp.com/pricing
- Freedom pricing: https://freedom.to/premium
- Freedom App Store: https://apps.apple.com/us/app/freedom-screen-time-control/id1269788228
- ScreenZen: https://screenzen.co/
- one sec FAQ/pricing: https://one-sec.app/faq/
- Jomo pricing: https://jomo.so/pricing
- Roots pricing: https://www.getroots.app/pricing
- Cold Turkey pricing: https://getcoldturkey.com/pricing/
- WakaTime pricing: https://wakatime.com/pricing
- RescueTime pricing: https://www.rescuetime.com/pricing
- Beeminder GitHub integration: https://www.beeminder.com/gitminder/
- Strut: https://www.strut-app.com/
- Earn Scroll: https://earnscroll.app/
- EarnIt: https://www.earn-it.uk/
- TapBlok: https://tapblok.com/
- GitHub Octoverse 2025: https://github.blog/news-insights/octoverse/octoverse-a-new-developer-joins-github-every-second-as-ai-leads-typescript-to-1/
- Stack Overflow 2025 Developer Survey press release: https://stackoverflow.co/company/press/archive/stack-overflow-2025-developer-survey/
- DataReportal Digital 2026 Global Overview: https://datareportal.com/reports/digital-2026-global-overview-report
- Reddit public signal, Opal price/Screen Time bypass: https://www.reddit.com/r/productivity/comments/1pi3ktg/if_opal_is_too_expensive_and_screen_time_is/
- Reddit public signal, Freedom critique: https://www.reddit.com/r/nosurf/comments/1r0kro7/i_tested_freedom_app_for_3_weeks_its_not_what_it/
- Reddit public signal, blocker subscription resistance: https://www.reddit.com/r/SideProject/comments/1sxyvgj/subscriptions_make_sense_for_apis_but_charging/
- Reddit public signal, earn-screen-time mixed reaction: https://www.reddit.com/r/Habits/comments/1r97xjj/i_found_an_app_that_literally_makes_you_earn_your/
