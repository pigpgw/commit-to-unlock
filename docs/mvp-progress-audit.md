# MVP Progress Audit

문서 상태: v0.1
점검일: 2026-05-03
역할: 현재 구현/문서/시장 근거를 한 번에 점검하고, 다음 MVP 진행 판단을 고정한다.

## 1. Executive Verdict

현재 판단은 `계속 개발`이다. 단, 다음 개발은 GitHub scoring이 아니라 Android 실기기 dogfood 검증이다.

이유:

- Android local blocker는 repo에서 빌드 가능한 상태이고, 선택 앱 감지/overlay/mock credit/policy exception/dogfood export/analyzer까지 들어왔다.
- 제품 포지션은 generic blocker가 아니라 `developer proof ledger + optional enforcement`로 잡아야 한다.
- GitHub scoring scaffold는 유지 가치가 있지만, 아직 모바일 차단 루프가 실제로 계속 켜둘 만큼 유용한지 검증되지 않았다.
- 경쟁 시장은 이미 붐빈다. 단순 차단 앱으로는 가격 저항이 크고, 개발자 proof ledger가 과금 명분이다.

현 단계의 가장 큰 리스크는 코드가 아니라 데이터 부재다. 실제 Android 기기에서 14일 dogfood TSV가 없으면 Gate B/C 판단은 아직 불가능하다.

## 2. Current Repo Snapshot

현재 tracked source는 77개 파일, 약 9.2k lines다. 핵심 구성은 아래와 같다.

| 영역 | 상태 | 판단 |
| --- | --- | --- |
| Android app | runnable prototype | 현재 유일한 실제 product surface |
| Android policy engine | implemented + unit tested | shared TypeScript policy와 reason code를 맞춰야 함 |
| Dogfood logging/export | implemented | target/reason/credit structured TSV로 개선됨 |
| Dogfood analyzer | implemented | Data Quality + Gate A/B/C snapshot 출력 |
| Shared package | implemented scaffold | mobile credit/policy contract 기준점 |
| Scoring package | pure rules scaffold | Sprint 4 후보. API/mobile과 아직 연결하지 않음 |
| API | `/health` only | GitHub placeholder 제거 상태가 맞음 |
| iOS | source/design skeleton | Xcode/entitlement 전에는 runnable 검증 불가 |
| CI | TypeScript + Android | main 기준 통과 |

## 3. MVP Progress

### Build-First MVP

| 단계 | 목표 | 현재 진행도 | 상태 |
| --- | --- | ---: | --- |
| Sprint 0 | repo 정리, CI, build baseline | 100% | 완료 |
| Sprint 1 | Android local blocker runnable | 80% | 구현됨, 실기기 반복 검증 필요 |
| Sprint 1.1 | policy controls, quest, exception, dogfood log | 85% | 구현됨, event quality 개선됨 |
| Sprint 1.2 | dogfood analyzer and Gate snapshot | 90% | 구현됨 |
| Sprint 2 | iOS FamilyControls prototype | 20% | 구조만 있음 |
| Sprint 3 | common local credit contract | 60% | shared contract 있음, iOS 적용 미완 |
| Sprint 4 | GitHub proof/scoring/API sync | 15% | scoring scaffold만 있음 |

### Real Product MVP

GitHub-backed 개인 개발자 MVP 기준으로 보면 아직 30% 전후다. 이유는 enforcement prototype은 빠르게 진전됐지만, 제품의 핵심 수익 명분인 proof ledger, GitHub enrichment, credit ledger persistence, mobile sync가 아직 구현 전이기 때문이다.

## 4. What Is Done

### Android

- 첫 실행 시 playful developer gate.
- Usage Access, Overlay, Notification permission 상태 표시.
- manual package target 입력과 recent foreground package 추가.
- `UsageStatsManager` 기반 foreground package 감지.
- foreground service 기반 monitor.
- selected target + credit 0일 때 full-screen overlay.
- strict mode에서 overlay test-credit shortcut 숨김.
- weekday/time/manual holiday/free day/emergency unlock/credit 정책.
- local mock credit add/spend/reset.
- blocked target foreground 60초 사용 시 1분 credit 자동 차감.
- daily quest plan과 mock proof completion.
- required quest 모두 완료 시 local midnight까지 free day.
- structured dogfood event log, TSV export, analyzer.

### Shared/API/Scoring

- TypeScript shared policy function과 tests.
- Android policy mirror와 unit tests.
- scoring package의 PR feature extraction/scoring pure function.
- API는 Fastify health route만 유지.
- 실제 enrichment 없는 GitHub webhook placeholder는 제거됨.

## 5. What Is Not Done

| 영역 | 미완료 | 이유 |
| --- | --- | --- |
| Android 실기기 dogfood | 14일 export 없음 | Gate B/C 판단 불가 |
| Android robustness | 제조사별 overlay/background 제한 미검증 | device matrix 필요 |
| iOS runnable build | Xcode project/entitlement 미검증 | 현재 Mac에 Xcode 앱 선택 필요 |
| GitHub App/OAuth | 구현 전 | Sprint 4 이전 금지 |
| webhook dedupe/enrichment | 구현 전 | files/reviews/checks/CI 없이 scoring하면 신뢰 하락 |
| credit ledger persistence | 구현 전 | 현재 Android local mock state만 있음 |
| server sync | 구현 전 | GitHub proof 이후 필요 |
| privacy/export/delete | 설계만 있음 | GitHub data 연결 전에는 구현 우선순위 낮음 |
| monetization | 구현 전 | Gate E 전까지 금지 |

## 6. Market And Platform Check

2026-05-03 기준 공개 자료 재확인 결과, 기존 방향은 유지한다.

| 근거 | 확인 내용 | 제품 판단 |
| --- | --- | --- |
| GitHub Octoverse 2025 | 180M+ developers, 36M+ new developers in one year, 43.2M merged PRs/month | GitHub proof supply는 충분하다. GitHub-first는 합리적이다. |
| Stack Overflow Survey 2025 | learner/community touchpoint로 Stack Overflow, GitHub public, YouTube, Reddit이 상위 | GTM은 앱 광고보다 developer community와 student/dev channels가 맞다. |
| UNESCO higher education | around 264M higher education students globally | student 시장은 크지만 미성년자/교육 데이터 이슈 때문에 1차가 아니다. |
| Freedom pricing | free tier와 cross-device premium이 존재 | 단순 blocker는 cross-device 기대치가 높다. Android-only 구독은 약하다. |
| one sec model | free one-app use, Pro unlocks multi-app and extra interventions | free/local entry와 paid advanced ledger/sync 구조가 더 현실적이다. |
| Android UsageStatsManager | usage APIs require `PACKAGE_USAGE_STATS` | 현재 Usage Access 기반 접근이 맞다. |
| Google Play Accessibility policy | autonomous Accessibility actions are tightly restricted | AccessibilityService를 MVP에서 제외한 결정은 유지한다. |
| Apple FamilyControls/ManagedSettings | entitlement와 privacy-preserving token model이 핵심 | iOS는 selected app shield만 약속해야 한다. 전체 폰 잠금 약속 금지. |

결론: 시장은 존재하지만 generic focus app으로는 약하다. “개발자가 만든 proof를 설명 가능한 leisure credit으로 바꾸는 ledger”가 차별점이다.

## 7. Unnecessary Scope To Stop

아래는 현재 만들면 안 된다.

| 범위 | 판단 | 이유 |
| --- | --- | --- |
| payments/subscription UI | 중지 | proof ledger 가치 검증 전 과금은 가짜 진전이다. |
| money stake/fine | 중지 | 결제/환불/분쟁/미성년자 리스크가 너무 큼. |
| school/parent/MDM | 중지 | 별도 규제/동의/배포 모델이 필요. |
| leaderboard | 중지 | developer self-regulation과 어긋나고 부정확한 비교를 만든다. |
| full-diff LLM judging | 중지 | privacy/cost/trust 리스크. rules-first가 우선. |
| GitLab/Bitbucket | 중지 | GitHub proof supply 검증 전 확장 불필요. |
| AccessibilityService | 중지 | 정책 부담 대비 현재 MVP 이득이 작다. |
| installed-app full scan | 중지 | package visibility 정책 부담. recent foreground/manual input으로 충분. |
| polished marketing site | 중지 | no-interview/no-landing 전략과 맞지 않음. |
| admin console/cohort | 중지 | personal dogfood가 먼저다. |

## 8. Missing Work That Matters

### Must Fix Before Sprint 4

1. Real device dogfood runbook
   14일 테스트를 어떻게 실행하고 어떤 TSV를 보관할지 문서화한다.

2. Dogfood decision record template
   analyzer 결과를 붙여 Gate A/B/C를 pass/fail/needs_data로 기록할 양식이 필요하다.

3. Android event store unit coverage
   6-column export, legacy parse, sanitize, max 1,000 event behavior를 테스트해야 한다.

4. Android device smoke checklist
   permission missing, foreground changed, overlay shown, credit spend, free day, emergency unlock을 실제 기기에서 체크한다.

5. GitHub Sprint 4 entry spec
   webhook dedupe, PR enrichment, feature vector persistence, credit ledger write를 한 번에 설계해야 한다.

### Should Add If Dogfood Passes

| 보완 | 이유 |
| --- | --- |
| local privacy screen | GitHub 전에도 log/export/delete 신뢰를 만든다. |
| dogfood export archive naming | 14일 기록 비교가 쉬워진다. |
| score simulator using fixture PRs | GitHub API 전 rules calibration 가능. |
| desktop/browser fallback spike doc | 모바일 enforcement 실패 시 피벗 지연을 줄인다. |

## 9. Gate Status

| Gate | 현재 상태 | 이유 | 다음 입력 |
| --- | --- | --- | --- |
| Gate A: enforcement viability | needs_data | 코드/CI는 통과했지만 실제 기기 반복 로그 부족 | Android 실기기 smoke + export |
| Gate B: dogfood need | needs_data | 14일 blocked attempt/override 데이터 없음 | 14일 TSV |
| Gate C: developer proof supply | needs_data | local mock proof는 가능하나 실제 GitHub/WakaTime 빈도 미측정 | 14일 dev activity log |
| Gate D: trust/privacy | needs_data | privacy 설계는 있으나 GitHub 연결 전 UI 없음 | metadata-only policy draft |
| Gate E: monetization | blocked | paid feature 검증 전 | ledger가 blocker 없이 가치 있는지 확인 |

## 10. Recommended Next 4 PRs

1. `docs/dogfood-runbook`
   14일 Android dogfood 실행법, 파일명 규칙, Gate decision template.

2. `test/android-event-store`
   DogfoodEventStore export/parse/sanitize/max events unit tests.

3. `feature/android-dogfood-review`
   앱 내부에 Data Quality/Gate summary를 최소 표시하거나 export 직후 확인 UX 추가.

4. `docs/github-sprint4-entry`
   GitHub App permissions, webhook dedupe, enrichment, ledger write, privacy policy를 Sprint 4 착수 전 설계.

이 4개 전에는 GitHub scoring 구현을 다시 시작하지 않는다.

## 11. Updated MVP Definition

현재 MVP는 두 층으로 분리한다.

### MVP-A: Local Enforcement Dogfood

목표: 개발자가 직접 쓰는 Android local blocker가 실제로 켜둘 만한지 확인한다.

출시 조건:

- Android real-device smoke checklist pass.
- 14일 TSV export 확보.
- analyzer Data Quality가 주요 target/reason/credit 이벤트에서 80% 이상.
- Gate A pass.
- Gate B pass 또는 mobile-first 유지 이유가 기록됨.
- Gate C에서 real GitHub PR-only가 부족하면 WakaTime/IDE/commit batch를 Sprint 4에 포함.

### MVP-B: Developer Proof Ledger

목표: GitHub proof가 leisure credit으로 바뀌고 mobile policy와 sync된다.

착수 조건:

- MVP-A의 Gate A/B/C 판단이 기록됨.
- GitHub Sprint 4 entry spec 작성.
- privacy/data retention policy draft 작성.
- scoring fixture set 20개 이상 확보.

## 12. Sources Checked

- GitHub Octoverse 2025: https://github.blog/news-insights/octoverse/octoverse-a-new-developer-joins-github-every-second-as-ai-leads-typescript-to-1/
- Stack Overflow Developer Survey 2025: https://survey.stackoverflow.co/2025
- UNESCO higher education: https://www.unesco.org/en/higher-education
- Freedom pricing: https://freedom.to/premium
- one sec Pro FAQ: https://tutorials.one-sec.app/en/articles/3036418
- Android UsageStatsManager: https://developer.android.com/reference/android/app/usage/UsageStatsManager
- Google Play AccessibilityService policy: https://support.google.com/googleplay/android-developer/answer/10964491
- Apple FamilyControls: https://developer.apple.com/documentation/familycontrols
- Apple ManagedSettings: https://developer.apple.com/documentation/managedsettings
