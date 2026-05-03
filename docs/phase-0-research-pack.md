# Commit-to-Unlock Phase 0 리서치 팩

상태: 보류. 현재 결정은 인터뷰/설문 없이 build-first로 진행하는 것이다. 이 문서는 나중에 고객 검증이 필요해졌을 때 다시 사용한다. 즉시 실행 계획은 [build-first-execution-plan.md](build-first-execution-plan.md)를 기준으로 한다.

목표: 2-3주 안에 “만들 가치가 있는가”가 아니라 “누가, 왜, 얼마에, 어떤 제어 강도로 쓸 것인가”를 검증한다.

## 1. 검증 목표

Phase 0에서 답해야 할 질문은 다섯 개다.

1. 개발자가 GitHub 활동을 스크린타임 보상 기준으로 쓰는 발상을 신뢰하는가?
2. private repo 접근과 code metadata 수집에 대한 거부선은 어디인가?
3. PR/CI/review 중심 평가가 커밋 수 기반보다 공정하게 느껴지는가?
4. 사용자가 실제로 줄이고 싶은 앱은 무엇이며, selected-app shield로 충분한가?
5. 개인 개발자, 학생, ADHD/도파민 조절 니즈, 부트캠프 중 어디가 초기 ICP인가?

## 2. 인터뷰 대상

총 30명 권장.

| 그룹 | 인원 | 조건 |
| --- | ---: | --- |
| 사이드프로젝트/오픈소스 개발자 | 8 | 주 1회 이상 GitHub 활동 |
| 취준생/학생 개발자 | 6 | 포트폴리오/과제 제출에 GitHub 사용 |
| ADHD/강한 도파민 조절 니즈 개발자 | 6 | 포커스 앱/차단 앱 사용 경험 |
| 현업 개발자 | 6 | PR/review/CI 워크플로 경험 |
| 부트캠프/코호트 운영자 | 4 | 학습 진도/과제 관리 경험 |

## 3. 인터뷰 스크립트

### 도입

- 오늘은 앱을 팔려는 인터뷰가 아니라, 문제와 사용 맥락을 확인하려는 인터뷰다.
- 정답은 없고, 부정적인 답변이 더 유용할 수 있다.
- private repo명, 회사명, 민감한 코드는 말하지 않아도 된다.

### 현재 문제

1. 평소 줄이고 싶은 앱이나 웹사이트가 있는가?
2. 그 앱을 열게 되는 순간은 언제인가? 예: 빌드 대기, 막힌 문제, 자기 전, 출근길.
3. 지금까지 써본 포커스/스크린타임 앱은 무엇인가?
4. 왜 계속 쓰지 못했는가?
5. 앱 차단이 너무 강해서 포기한 적이 있는가?

### 개발 워크플로

6. 개발 활동은 주로 GitHub, GitLab, Bitbucket 중 어디에 남는가?
7. 개인 프로젝트에서도 PR을 쓰는가?
8. merge, CI 통과, review, issue link 중 어떤 신호가 “일했다”는 증거로 가장 공정한가?
9. 커밋 수로 보상한다면 어떤 악용이 가능하다고 보는가?
10. 타인의 코드 리뷰도 보상 대상이어야 하는가?

### 신뢰와 데이터

11. GitHub App을 연결한다면 public repo만 허용할 것인가, private repo도 가능한가?
12. 앱이 raw diff를 저장하지 않고 feature만 저장한다면 신뢰가 올라가는가?
13. LLM이 코드 일부를 볼 수 있다는 설명을 들으면 허용할 수 있는가?
14. 점수 이유가 설명되면 AI 판정을 믿을 수 있는가?
15. 판정이 억울할 때 appeal이 있으면 충분한가?

### 보상/차단

16. 하루에 earn 가능한 최대 시간이 몇 분이면 적절한가?
17. 10분, 25분, 45분, 60분 tier가 직관적인가?
18. 보상은 앱 전체 접근, 특정 앱 접근, 웹사이트 접근 중 무엇이어야 하는가?
19. emergency override는 필요하다고 보는가?
20. override에 이유 입력/횟수 제한/다음날 cap 감소 중 어떤 제약이 납득되는가?

### 가격

21. 이 앱이 실제로 효과가 있다면 월 얼마까지 낼 수 있는가?
22. 연간 결제라면 얼마가 적절한가?
23. 학생 할인 가격은 얼마가 적절한가?
24. lifetime 가격이 있으면 선호하는가, 아니면 서버형 앱이라 구독이 납득되는가?
25. 금전 예치금/벌금 기능에 관심이 있는가? 불편하거나 위험하게 느껴지는가?

### 마무리

26. 이 제품을 한 문장으로 친구에게 설명한다면 어떻게 말하겠는가?
27. 절대 쓰지 않을 이유가 있다면 무엇인가?
28. 베타에 참여할 의향이 있는가?
29. 베타에서 꼭 있어야 하는 기능 하나는 무엇인가?
30. 이 제품이 실패한다면 가장 가능성 높은 이유는 무엇인가?

## 4. 설문 초안

권장 도구: Google Forms, Tally, Typeform. 응답 목표 100명.

### 스크리닝

- 현재 직군/상태: 현업 개발자, 학생, 취준생, 부트캠프 수강생, 기타
- 주 사용 Git 플랫폼: GitHub, GitLab, Bitbucket, 기타, 사용 안 함
- 주당 코드 활동 빈도: 거의 없음, 1-2일, 3-4일, 5일 이상
- 포커스/차단 앱 사용 경험: 없음, 과거 사용, 현재 사용

### 문제 강도

1-5점 척도:

- 스마트폰/웹 사용 때문에 개발 시간이 줄어든다.
- 기존 스크린타임/포커스 앱은 오래 지속하기 어렵다.
- 작업을 완료해야만 방해 앱을 여는 방식에 관심이 있다.
- 개발 활동을 자동 검증해 보상하는 방식이 자기신고보다 낫다.

### 신뢰/데이터

복수 선택:

- 허용 가능한 데이터: public repo metadata, private repo metadata, changed file list, diff summary, raw diff, CI/review metadata
- 허용 불가 데이터: repo 내용, 회사 private repo, reviewer comments, file names, none

### scoring 선호

가장 공정한 순서로 정렬:

- merged PR
- PR opened
- commit count
- changed lines
- tests added
- CI passed
- review approval
- issue linked
- code review submitted

### 가격

Van Westendorp:

- 너무 싸서 의심되는 연간 가격
- 싸다고 느끼는 연간 가격
- 비싸지만 고려 가능한 연간 가격
- 너무 비싸서 포기하는 연간 가격

## 5. Fake-Door 랜딩 실험

### 랜딩 구조

Hero:

> Ship code. Earn guilt-free screen time.

Subcopy:

> Commit-to-Unlock converts verified GitHub activity into transparent credits for selected app and website access.

Primary CTA:

> Join private beta

Secondary CTA:

> See scoring examples

### 가격 실험

3개 랜딩 variant를 만든다.

| Variant | Pro 가격 | 메시지 |
| --- | ---: | --- |
| A | $49.99/year | 저항 낮은 개인 개발자용 |
| B | $59.99/year | 기존 보고서 기준 |
| C | $79.99/year | 고가 focus app 대비 premium |

측정:

- 방문 -> CTA 클릭
- CTA -> GitHub 연결 의향 체크
- email capture
- “private repo 허용 가능” 체크율
- 가격 페이지 이탈률

## 6. Competitor Teardown 템플릿

제품별로 30분씩 실제 사용 또는 스토어/웹 조사.

| 항목 | 기록 |
| --- | --- |
| 제품명 | |
| 가격 | |
| 무료 플랜 실사용 가능성 | |
| onboarding 단계 수 | |
| 권한 요청 방식 | |
| 차단 강도 | |
| 우회 방지 방식 | |
| 차단 화면 메시지 | |
| override/긴급 해제 | |
| 데이터/프라이버시 문구 | |
| trust를 높이는 요소 | |
| 불쾌하거나 위험한 요소 | |
| Commit-to-Unlock이 배워야 할 점 | |
| 차별화 가능한 빈틈 | |

필수 teardown 대상:

- Opal
- ScreenZen
- one sec
- Freedom
- Beeminder GitHub
- WakaTime
- OurPact
- Qustodio
- Cold Turkey 또는 FocusMe
- Forest 또는 Habitica

## 7. PR 라벨링 가이드 v0

목표: 200-500개 PR을 사람이 먼저 라벨링해서 scoring rule을 보정한다.

### 라벨

| 라벨 | 의미 |
| --- | --- |
| no_credit | 보상 없음 |
| small_credit | 10분 수준 |
| medium_credit | 25분 수준 |
| high_credit | 45분 수준 |
| max_credit | 60분 수준 |
| needs_review | 자동 판정 보류 |

### 라벨링 기준

보상 높음:

- source 변경이 명확하다.
- 테스트/CI/문서/이슈 링크가 있다.
- 리뷰 코멘트나 승인 흐름이 있다.
- 한 번에 이해 가능한 목적이 있다.

보상 낮음:

- typo, whitespace, formatting only
- lockfile/generated/vendor 중심
- README badge만 변경
- 빈 커밋/동일 patch 반복
- 대량 파일 이동/rename만 존재

보류:

- 보안/인증/결제 등 민감 변경
- 매우 큰 diff
- generated와 source가 섞여 판정 어려움
- revert 여부가 불명확함

## 8. Phase 0 성공/실패 판정

Go:

- GitHub 연결 의향 60% 이상
- private repo metadata 허용 35% 이상 또는 public-only로도 초기 사용 가능성 확인
- scoring explanation 신뢰 60% 이상
- beta 참여 email 전환 8% 이상
- Pro 연 $49.99 이상 지불 의향 10% 이상

Pivot:

- GitHub 연결 거부가 높지만 WakaTime/IDE local 측정은 허용됨
- PR 중심이 너무 무겁고 commit batch 선호가 압도적임
- 모바일 차단보다 browser extension/web blocker 수요가 강함

Stop:

- 핵심 세그먼트가 기존 포커스 앱으로 충분하다고 답함
- 개발 활동 보상을 “감시/죄책감”으로 받아들임
- GitHub 연결과 screen-time 권한 둘 중 하나라도 대부분 거부함
