# Commit-to-Unlock 보강 조사 및 실행 기획

조사일: 2026-05-03  
기본 가정: 글로벌 B2C 개인 개발자에서 시작하고, 한국/미국/EU 규제는 별도 리스크로 관리한다.  
핵심 결론: 만들 가치가 있다. 다만 “커밋하면 잠금 해제”가 아니라 “검증 가능한 개발 기여를 크레딧 장부로 전환하고, 사용자가 고른 방해 앱 접근을 정책적으로 관리하는 제품”으로 좁혀야 한다.

현재 상태: 배경 리서치 자료. 실행 기준은 [app-design.md](app-design.md), [decision-log.md](decision-log.md), [build-first-execution-plan.md](build-first-execution-plan.md)를 따른다. 이 문서의 인터뷰/설문/fake-door 항목은 보류한다.

## 1. 보강된 전략 결론

기존 보고서의 큰 방향은 맞다. 하지만 더 조사해 보니 포지셔닝은 더 날카롭게 잡아야 한다.

첫째, 직접 경쟁은 단순 차단 앱만이 아니다. Beeminder는 이미 GitHub 연동으로 커밋/이슈 목표를 추적하고, 실패 시 실제 돈을 청구하는 구조를 제공한다. WakaTime은 IDE 기반 개발 활동 측정을 제공한다. 따라서 이 제품의 차별점은 “GitHub 연동” 자체가 아니라, `PR 품질 신호 + credit ledger + 선택 앱 shield/block + 설명 가능한 판정`의 결합이어야 한다.

둘째, 초기 MVP를 `merged PR only`로 너무 강하게 제한하면 솔로 개발자와 학습자의 activation이 떨어질 수 있다. 권장안은 `provisional credit`과 `confirmed credit`의 2단계다.

- `provisional credit`: PR 생성, PR 업데이트, 트래킹 repo의 의미 있는 commit batch에 낮은 임시 크레딧을 준다.
- `confirmed credit`: merge, CI 통과, 리뷰/이슈 링크/테스트 신호가 있을 때 확정 크레딧으로 승격한다.
- `clawback`: 빠른 revert, 중복 patch, generated/lockfile 위주 작업은 회수하거나 상한을 낮춘다.

셋째, 모바일 집행 약속은 반드시 낮춰야 한다. iOS와 Android 모두 일반 소비자 앱이 “기기 전체 강제 잠금”을 안정적으로 약속하기 어렵다. 제품 문구는 `phone lock`이 아니라 `selected app and website shielding`이어야 한다.

넷째, 미성년자/학교/부모용은 1차 제품에서 제외하는 편이 맞다. 이 세그먼트는 매력적이지만 COPPA, FERPA, GDPR 아동 동의, 한국 만 14세 미만 법정대리인 동의, App Store Kids Category, Android parental/enterprise policy까지 겹친다.

## 2. 핵심 가설

아래 검증 방법은 초기 리서치 초안이며 현재 실행하지 않는다. 현재는 build-first 검증으로 대체한다.

### 사업 가설

| 가설 | 검증 방법 | 실패 기준 |
| --- | --- | --- |
| 개발자는 일반 포커스 앱보다 “코드 활동으로 leisure time을 얻는 방식”을 더 신뢰한다. | 30명 인터뷰, fake-door 랜딩, 가격 테스트 | GitHub 연결 의향이 40% 미만 |
| 커밋 수보다 PR/테스트/리뷰 기반 평가가 공정하게 느껴진다. | 샘플 PR 20개 점수 공개 후 공정성 평가 | “이유가 납득된다” 응답 60% 미만 |
| 선택 앱 차단만으로도 충분한 행동 변화가 생긴다. | 2주 alpha, earned-to-open conversion 측정 | 차단 순간 override가 50% 초과 |
| 부트캠프/코호트는 “감시”보다 “학습 리듬 관리”로 팔아야 도입된다. | 코호트 운영자 10명 인터뷰 | 관리자가 코드 품질 평가 도구로 오해하거나 거부 |

### 제품 가설

| 가설 | 제품 실험 |
| --- | --- |
| `score explanation`이 없으면 AI 판정 신뢰가 낮다. | 같은 점수라도 설명형 shield vs 단순 shield A/B |
| `merged PR required`는 악용을 줄이지만 초기 보상 속도를 늦춘다. | PR-only vs commit+PR mixed scoring |
| 하루 상한 cap이 없으면 보상 인플레이션과 어뷰징이 생긴다. | cap 60/90/120분 cohort 비교 |
| 금전 스테이크는 전환에는 강하지만 분쟁/환불/미성년자 리스크가 크다. | 1차 MVP 제외, 성인 웹 실험으로만 검토 |

## 3. 시장 근거 업데이트

### 개발자 이벤트 볼륨

GitHub Octoverse 2025는 시장 상한을 뒷받침한다. GitHub는 2025년에 3,600만 명 이상 신규 개발자가 유입되어 1억 8천만 명 이상의 개발자 규모를 기록했고, 월평균 4,320만 건의 PR이 merge되었다. 2025년 커밋은 약 9.86억 건이었다. 이 숫자는 “개발 활동을 보상 이벤트로 쓰는 제품”에 충분한 원천 이벤트가 있다는 뜻이다.

다만 이 숫자를 paying TAM으로 쓰면 안 된다. 실제 SAM은 다음 조건을 모두 만족하는 사람이다.

- Git 기반 활동을 주기적으로 남긴다.
- GitHub/GitLab/Bitbucket 접근 권한을 앱에 줄 의향이 있다.
- 스마트폰 방해 앱을 줄이려는 고통이 명확하다.
- 자기통제 앱에 돈을 낼 의향이 있다.

### 학습자/학생 시장

UNESCO는 2025년 전 세계 고등교육 재학생을 2억 6,400만 명으로 집계했다. Stack Overflow 2025 자료는 개발자 커뮤니티 접점에서 Stack Overflow, GitHub, YouTube가 상위임을 보여준다. 즉 개발자용 GTM은 일반 앱 광고보다 개발 커뮤니티, GitHub Marketplace, 대학 CS 동아리, 해커톤, 부트캠프 제휴 쪽이 맞다.

학생 시장은 크지만 첫 진입 시장으로는 복잡하다. 학생 중 미성년자, 학교 계약, 학습 기록, 보호자 동의가 들어가면 법무/보안 비용이 급격히 증가한다. 따라서 학생용은 `개인 학생 할인`까지만 1차에 허용하고, 학교 관리형은 2차 이후가 맞다.

### 수정된 세그먼트 우선순위

| 우선순위 | 세그먼트 | 이유 | 초기 메시지 |
| --- | --- | --- | --- |
| 1 | 사이드프로젝트/오픈소스 개인 개발자 | GitHub 연결 부담이 낮고 자발적 사용 가능 | Ship code, earn time |
| 2 | ADHD/도파민 조절 니즈가 있는 개발자 | 문제 강도가 높고 포커스 앱 지불 의향이 있음 | Turn verified work into guilt-free breaks |
| 3 | 취준생/학생 개발자 | 포트폴리오와 GitHub 활동이 연결됨 | Build your portfolio before you scroll |
| 4 | 부트캠프/코호트 | seat 기반 매출 가능, 운영자 도입 필요 | Learning rhythm, not surveillance |
| 5 | 부모/학교 관리형 | 강한 통제 니즈가 있으나 규제/MDM 필요 | 별도 제품으로 분리 |

## 4. 경쟁 구도 재정의

### 경쟁 카테고리

| 카테고리 | 제품 | 시사점 |
| --- | --- | --- |
| 강한 차단/포커스 | Opal, Freedom | 차단 UX와 가격 벤치마크. 하지만 “해제 자격”은 개발 산출물과 연결되지 않는다. |
| 마찰형 습관 교정 | one sec, ScreenZen | 무료/저가 대안이 강하다. 단순 차단만으로는 유료 전환이 어렵다. |
| 부모 제어 | OurPact, Qustodio, Google Family Link | 관리형/아동 시장은 기능과 규제가 완전히 다르다. |
| 행동 계약/스테이크 | Beeminder, StickK, Forfeit | Beeminder의 GitHub 연동은 직접적으로 주의해야 한다. |
| 개발 활동 분석 | WakaTime, CodeTime류 | “개발 활동 측정” 자체는 이미 있다. 보상/차단 연결이 차별점이다. |

### 중요한 경쟁사 업데이트

| 제품 | 확인한 포인트 | Commit-to-Unlock 대응 |
| --- | --- | --- |
| Beeminder GitHub | GitHub commits/issues 목표를 추적하고 실패 시 실제 돈을 청구한다. | 커밋 수 기반이면 차별화 약함. PR 품질/CI/리뷰/크레딧 장부/앱 shield가 필요. |
| WakaTime | IDE 플러그인 기반 개발 시간/언어/프로젝트 통계를 제공한다. | GitHub API만으로 부족하면 WakaTime 연동은 보조 데이터 소스로 검토 가능. |
| ScreenZen | 무료, donation-supported, lock mode를 제공한다. | B2C 가격 저항을 낮게 봐야 한다. 무료 플랜이 실사용 가능해야 한다. |
| Opal | 연 $99.99 수준의 고가 Pro 포지션. | 고가 시장은 존재하지만 강한 브랜드/UX 없이는 바로 따라가기 어렵다. |
| one sec | 마찰형 UX와 과학적 메시지, 저가/연간 플랜. | “차단 강도”보다 “행동 전환” 메시지가 중요하다. |

### 차별화 문장

나쁜 문장:

> Commit to Unlock is a focus app that unlocks your phone when you commit code.

권장 문장:

> Commit-to-Unlock turns verified GitHub work into a transparent credit ledger for selected app and website access.

개발자용 짧은 문구:

- Ship code, earn time.
- Merged PRs unlock guilt-free breaks.
- Code-backed screen time for developers.
- Earn leisure credits with verified development work.

## 5. 제품 범위 보강

### MVP는 세 개 제품이 아니라 하나여야 한다

초기에는 다음 기능만 만든다.

- GitHub App 연동
- PR/commit event ingestion
- feature extraction
- rules-first scoring
- credit ledger
- iOS 또는 Android 한 플랫폼의 selected-app enforcement
- 오늘의 크레딧, 최근 판정, 왜 몇 분인지 설명
- override/appeal 최소 버전

제외한다.

- GitLab/Bitbucket
- 부모/학교 관리
- MDM
- 금전 스테이크
- leaderboard
- 복잡한 ML 모델
- raw source code 장기 보관

### MVP scoring subject

| 이벤트 | 크레딧 성격 | 권장 처리 |
| --- | --- | --- |
| PR opened | provisional | 최소 5~10분, daily cap 낮게 |
| PR updated | provisional | 의미 있는 diff 증가 시만 |
| PR merged | confirmed | 메인 보상 이벤트 |
| Commit batch without PR | provisional | 솔로/학습자용 fallback |
| Review submitted | confirmed or bonus | 타인 코드 리뷰도 개발 기여로 인정 가능 |
| CI passed | multiplier | 단독 보상보다 신뢰도 보정 |
| Revert detected | negative/clawback | 확정 크레딧 일부 회수 |

### 루브릭 권장안

100점 점수는 사용자에게 그대로 노출하지 말고, 내부 판정과 설명에만 쓴다. 사용자는 `earned minutes`와 `why`를 본다.

| 항목 | 배점 | 비고 |
| --- | ---: | --- |
| 실질 diff | 20 | source/test/config/docs 분류, generated 제외 |
| 검증 신호 | 15 | test, CI, typecheck, lint |
| 문제 맥락 | 10 | issue link, branch naming, PR description |
| 리뷰/협업 | 20 | approvals, comments, resolved discussions |
| 난이도/범위 | 15 | cross-layer, multi-file, migration 등 |
| 라이프사이클 | 10 | open-update-review-merge 흐름 |
| 신뢰도/어뷰징 저항 | 10 | duplicate patch, revert, burst, bot 제외 |

환산:

| 점수 | 보상 |
| --- | --- |
| 0-24 | 없음 |
| 25-44 | 10분 |
| 45-64 | 25분 |
| 65-79 | 45분 |
| 80+ | 60분 |

권장 일일 cap: 기본 60분, Pro 90분, 사용자가 낮출 수 있음. 높이는 기능은 신중해야 한다.

### 사용자에게 보여줄 설명 형식

```
45분 적립됨

이유:
- 6개 source file과 3개 test file 변경
- CI 통과
- linked issue 포함
- 리뷰 코멘트 7개와 승인 1개

감점:
- 없음
```

점수 원인을 설명하지 못하면 개발자는 AI 판정을 신뢰하지 않는다. Stack Overflow 2025 조사에서도 개발자의 AI 신뢰는 낮아지고 있어, “AI가 알아서 판단”보다 “규칙 기반 판정 + 제한적 AI 설명”이 낫다.

## 6. 기술 타당성 보강

### Git provider 순서

| 순서 | provider | 판단 |
| --- | --- | --- |
| 1 | GitHub | 시장/문서/API/webhook/PR 생태계가 가장 좋다. MVP는 GitHub-only. |
| 2 | GitLab | MR, diffs, approvals, discussions, webhooks 모두 가능하나 SaaS/self-managed 변형이 많다. |
| 3 | Bitbucket | 가능하지만 초기 B2C 개발자 풀이 작고 기업/Atlassian 성격이 강하다. |

### GitHub 구현 원칙

- GitHub App 우선. OAuth App보다 권한 범위와 설치 단위 관리가 낫다.
- Webhook-first. 이벤트 수신 후 필요한 PR만 enrichment한다.
- Webhook은 10초 내 응답하고 queue로 넘긴다.
- raw payload는 장기 저장하지 말고 payload hash와 필요한 snapshot만 저장한다.
- diff 전문을 LLM에 보내지 않는다. 먼저 feature vector를 만들고, 필요한 hunk만 제한적으로 사용한다.

### 개인정보/코드 보안 원칙

이 제품은 private repo 접근을 요구할 가능성이 높다. 그래서 보안 설계가 제품 신뢰의 일부다.

권장 원칙:

- 기본은 metadata/features 저장, raw diff 장기 저장 금지
- raw diff 임시 보관 기간 7일 이하
- provider token은 KMS/secret manager로 암호화
- repo allowlist 방식
- uninstall/revoke 시 provider token 즉시 폐기
- user data export/delete 제공
- private repo명 표시를 숨기는 privacy mode 제공
- LLM 사용 시 source code 전송 여부를 명확히 opt-in

### iOS 집행 범위

Apple FamilyControls 문서상 개인 사용자도 기기 소유자 승인으로 Family Controls authorization을 받을 수 있고, 아동 기기는 보호자 승인이 필요하다. FamilyActivitySelection은 앱/도메인을 opaque token으로 다루며, Device Activity는 privacy-preserving activity monitoring을 제공한다. Managed Settings/Shield extension은 앱/웹사이트 shield UX를 구성한다.

중요한 제약:

- 배포 전 Family Controls entitlement 승인이 필요하다.
- Screen Time API extension bundle도 entitlement 요청이 필요하다.
- 소비자 앱에서 전체 기기 잠금/비제거/강제 MDM 수준 통제를 약속하면 위험하다.
- child/parent mode에서는 부모 승인 시 앱 삭제 방지 등 일부 우회 방지가 가능하지만, 이 문맥을 일반 성인 자기관리 모드에 그대로 가져오면 안 된다.

### Android 집행 범위

Android는 UsageStatsManager로 앱 사용 이벤트/통계를 볼 수 있지만 `PACKAGE_USAGE_STATS` 권한과 사용자 동의가 필요하다. Accessibility API는 정책상 매우 민감하다. Google Play 정책은 사용자의 허가 없는 설정 변경, privacy control 우회, uninstall/disable 방지를 금지하고, 예외는 parental control 또는 enterprise management software 쪽에 가깝다.

권장:

- B2C: Usage Access + 명시적 overlay/interruption/block flow
- Accessibility는 필요한 경우 최소 권한, prominent disclosure, Play listing 설명
- uninstall 방지/설정 변경 방지는 B2C에서 약속하지 않음
- 강한 kiosk/lock task는 DPC/managed device 전용 SKU에서만 검토

## 7. 규제 및 심사 리스크

### 1차 MVP에서 피해야 할 것

- 13세 미만 사용자 대상 마케팅
- 학교가 학생 기록을 관리하는 형태
- 보호자 대시보드
- 앱 삭제 방지 약속
- 벌금/예치금
- raw source code를 LLM에 기본 전송
- “AI가 휴대폰을 잠근다” 같은 문구

### 지역별 주의

| 지역 | 리스크 | 제품 대응 |
| --- | --- | --- |
| 미국 | COPPA, FERPA | 13세 미만 제외. 학교 계약 전 DPA/FERPA 검토. |
| EU | GDPR Article 8 아동 동의, 민감한 profiling 우려 | age gate, consent/legal basis, DPA, deletion/export |
| 한국 | 만 14세 미만 법정대리인 동의 | 한국 B2C도 아동 모드 제외 |
| Apple | Family Controls entitlement, Kids Category, public API only | consumer self-control wording, entitlement early 신청 |
| Google Play | Accessibility/usage disclosure, uninstall 방지 제한 | prominent disclosure, parental/enterprise SKU 분리 |

## 8. 가격/수익화 보강

초기 가격은 Opal처럼 바로 고가로 가기보다, 개발자 특화 가치와 focus 앱 가격 저항을 함께 봐야 한다.

권장 초기 가격:

| 플랜 | 가격 | 비고 |
| --- | ---: | --- |
| Free | $0 | GitHub 1계정, 1 repo, 낮은 daily cap, 기본 scoring |
| Pro | $5.99/월 또는 $49.99/년 | 다중 repo, 상세 설명, 고급 정책, appeal |
| Student | $29.99/년 | 교육 인증 또는 self-declared student beta |
| Cohort beta | $3-5/seat/month | 관리자 콘솔은 retention 검증 이후 |
| School/Enterprise | 별도 견적 | SSO/MDM/legal/security 필요 |

가격 실험:

- 랜딩에서 $49.99/y, $59.99/y, $79.99/y fake-door 테스트
- Student는 $29.99/y와 $3.99/m 비교
- Pro trial은 7일보다 “첫 confirmed PR까지 무료”가 더 제품에 맞음
- lifetime은 초기에 피한다. 서버/API/LLM 비용이 있는 제품이라 장기 부채가 된다.

## 9. GTM 보강

### 1차 채널

- GitHub Marketplace
- Product Hunt
- Hacker News Show HN
- Reddit: r/SideProject, r/productivity, r/ADHD_Programmers 계열
- dev.to, Hashnode
- 대학 CS 동아리/해커톤
- 한국 개발자 커뮤니티: OKKY, 디스코드, 부트캠프 커뮤니티

### 1차 콘텐츠

콘텐츠는 기능 설명보다 “공정한 scoring 사례”가 좋다.

- “왜 이 PR은 45분이고 이 커밋은 0분인가”
- “Commit spam이 왜 통하지 않는가”
- “AI generated code도 인정되는가: 테스트/리뷰/merge가 기준이다”
- “개발자가 납득할 수 있는 screen time rule engine 만들기”

### 피해야 할 메시지

- “AI가 개발자를 감시한다”
- “휴대폰을 완전히 잠근다”
- “아이를 강제로 코딩하게 한다”
- “커밋만 하면 SNS를 열어준다”

## 10. 검증 로드맵

### Phase 0: 조사/검증, 2-3주

산출물:

- 30명 인터뷰
- 100명 설문
- competitor teardown 10개
- GitHub App permission 설계
- Apple Family Controls entitlement 신청
- scoring rubric v0
- 200개 PR 라벨링 기준

성공 기준:

- 인터뷰 중 50% 이상이 현재 포커스/스크린타임 문제를 돈 내고 해결 중이거나 해결 의향 있음
- GitHub 연결 거부율 40% 이하
- scoring explanation 신뢰도 60% 이상

### Phase 1: Scoring simulator, 3-4주

모바일 차단 없이 웹에서 GitHub 연결과 scoring만 검증한다.

산출물:

- GitHub App
- webhook receiver
- PR enrichment
- feature extractor
- credit ledger
- scoring explanation UI

성공 기준:

- 연결 후 첫 scored event 도달률 40% 이상
- 판정 appeal intent 20% 이하
- “이 점수를 행동 보상에 써도 되겠다” 응답 60% 이상

### Phase 2: Mobile enforcement spike, 4-6주

목표는 앱스토어 출시가 아니라 플랫폼 리스크 제거다.

산출물:

- iOS FamilyControls/ManagedSettings prototype
- Android UsageStats/overlay prototype
- selected app blocking
- credit sync
- override

성공 기준:

- iOS entitlement/TestFlight 경로 확인
- Android Play policy wording 정리
- 20명 alpha에서 block/unlock loop가 일주일 이상 동작

### Phase 3: Private alpha, 6-8주

산출물:

- 50-100명 개인 개발자 alpha
- daily cap/override/appeal
- anti-abuse rules
- Stripe/RevenueCat 결제 검증은 retention 이후로 보류

성공 기준:

- D7 retention 35% 이상
- D30 retention 15% 이상
- weekly earned minutes 중앙값 60분 이상
- override frequency 주 3회 이하
- voluntary enabled days 주 4일 이상

### Phase 4: Cohort beta, 8-12주

개인 retention이 확인된 뒤에만 진행한다.

산출물:

- cohort admin
- seat activation
- cohort policy template
- privacy mode
- educator report

성공 기준:

- seat activation 60% 이상
- cohort 4주 유지율 50% 이상
- 운영자 재구매 의향 50% 이상

## 11. 다음 조사 체크리스트

### 고객 조사 질문

- 지금 줄이고 싶은 앱은 무엇인가?
- 이미 쓰는 차단/포커스 앱은 무엇이고 왜 실패했는가?
- GitHub/GitLab 연결을 허용할 수 있는 repo 범위는 어디까지인가?
- private repo metadata 수집에 대한 허용선은 어디인가?
- 커밋, PR, review, CI 중 어떤 신호가 가장 공정하다고 느끼는가?
- AI 설명이 있으면 신뢰가 올라가는가, 오히려 불편한가?
- 하루 몇 분 보상이 적절한가?
- override는 어느 정도까지 허용되어야 하는가?
- 월/연 가격 저항선은 어디인가?
- 부모/코치/관리자가 보는 리포트를 허용할 수 있는가?

### 경쟁 조사 체크리스트

- Opal: onboarding, hard lock, pricing, cancellation friction
- ScreenZen: 무료 기능 범위, lock mode, donation UX
- one sec: intervention setup, scientific claim, strict block
- Freedom: multi-device blocking, locked mode
- Beeminder: GitHub integration, pledge mechanics, failure UX
- WakaTime: 개발 활동 측정 granularity, API 가능성
- OurPact/Qustodio: parent admin, safeguards, pricing

### 기술 조사 체크리스트

- GitHub App permission 최소 세트
- GitHub GraphQL reviewThread/resolved state 필요 여부
- generated/lock/vendor 파일 탐지 rule
- patch hash normalization
- CI status/check runs API
- iOS entitlement 승인 예상 기간과 extension bundle별 요구
- Android Play policy review wording
- LLM provider에 source code 전송하지 않는 scoring path

## 12. 보강된 최종 권고

1. 첫 제품은 GitHub-only로 간다.
2. 커밋 수가 아니라 PR/리뷰/테스트/CI/이슈 기반 신호를 중심에 둔다.
3. 솔로 개발자 activation을 위해 commit batch provisional credit은 허용한다.
4. “점수”보다 “credit ledger”를 제품 핵심 모델로 삼는다.
5. AI는 판정자가 아니라 설명자와 edge-case reviewer로 둔다.
6. iOS/Android는 전체 잠금이 아니라 선택 앱/웹사이트 shield로 약속한다.
7. 미성년자/학교/부모/MDM은 MVP 이후 별도 SKU로 분리한다.
8. 금전 스테이크는 성인 웹 실험 전까지 제외한다.
9. 지금은 고객 인터뷰가 아니라 Android 실기기 차단 검증부터 만든다.
10. GitHub scoring simulator와 Apple Family Controls entitlement는 모바일 차단 가능성 확인 후 순차 진행한다.

## 13. 주요 출처

- GitHub Octoverse 2025: https://github.blog/news-insights/octoverse/octoverse-a-new-developer-joins-github-every-second-as-ai-leads-typescript-to-1/
- UNESCO higher education enrollment: https://www.unesco.org/en/articles/record-number-higher-education-students-highlights-global-need-recognition-qualifications
- Stack Overflow Developer Survey 2025 summary: https://stackoverflow.blog/2025/12/29/developers-remain-willing-but-reluctant-to-use-ai-the-2025-developer-survey-results-are-here/
- GitHub REST API rate limits: https://docs.github.com/en/rest/using-the-rest-api/rate-limits-for-the-rest-api
- GitHub webhook best practices: https://docs.github.com/en/webhooks/using-webhooks/best-practices-for-using-webhooks
- GitHub pull request API: https://docs.github.com/en/rest/pulls/pulls
- GitHub pull request reviews API: https://docs.github.com/en/rest/pulls/reviews
- GitLab webhook events: https://docs.gitlab.com/user/project/integrations/webhook_events/
- GitLab.com rate limits: https://docs.gitlab.com/user/gitlab_com/
- GitLab merge requests API: https://docs.gitlab.com/api/merge_requests/
- GitLab discussions API: https://docs.gitlab.com/api/discussions/
- GitLab merge request approvals API: https://docs.gitlab.com/api/merge_request_approvals/
- Bitbucket API request limits: https://support.atlassian.com/bitbucket-cloud/docs/api-request-limits/
- Bitbucket pull request API: https://developer.atlassian.com/cloud/bitbucket/rest/api-group-pullrequests/
- Apple Family Controls: https://developer.apple.com/documentation/familycontrols
- Apple configuring Family Controls: https://developer.apple.com/documentation/xcode/configuring-family-controls
- Apple FamilyActivitySelection: https://developer.apple.com/documentation/familycontrols/familyactivityselection
- Apple Device Activity: https://developer.apple.com/documentation/deviceactivity
- Apple Managed Settings overview: https://developer.apple.com/documentation/managedsettings/connectionwithframeworks
- Apple App Review Guidelines: https://developer.apple.com/app-store/review/guidelines/
- Apple MDM commands: https://developer.apple.com/documentation/devicemanagement/commands-and-queries
- Apple device lock deployment guide: https://support.apple.com/guide/deployment/depb980a0be4/web
- Android UsageStatsManager: https://developer.android.com/reference/android/app/usage/UsageStatsManager
- Google Play Developer Program Policy: https://support.google.com/googleplay/android-developer/answer/16528695
- Android lock task mode: https://developer.android.com/work/dpc/dedicated-devices/lock-task-mode
- Android device admin deprecation: https://developers.google.com/android/work/device-admin-deprecation
- Opal pricing: https://opalapp.com/discount
- one sec App Store: https://apps.apple.com/us/app/one-sec-screen-time-focus/id1532875441
- one sec FAQ: https://one-sec.app/ko/faq
- ScreenZen: https://screenzen.co/
- Freedom pricing: https://freedom.to/premium
- OurPact pricing: https://www.ourpact.com/pricing
- Qustodio premium comparison: https://www.qustodio.com/en/difference-between-qustodio-free-and-qustodio-premium/
- Beeminder GitHub integration: https://www.beeminder.com/gitminder
- Beeminder pricing: https://www.beeminder.com/premium
- WakaTime pricing: https://wakatime.com/pricing
- FTC COPPA rule: https://www.ftc.gov/legal-library/browse/rules/childrens-online-privacy-protection-rule-coppa
- FTC COPPA FAQ: https://www.ftc.gov/tips-advice/business-center/guidance/complying-coppa-frequently-asked-questions
- U.S. Department of Education FERPA FAQ: https://www.ed.gov/about/contact-us/faqs/Student%20Records%20and%20Privacy
- GDPR official text: https://eur-lex.europa.eu/eli/reg/2016/679/oj
- Korean PIPA English text: https://elaw.klri.re.kr/eng_service/lawTwoView.do?hseq=62389
