# Commit-to-Unlock MVP PRD

문서 상태: v0.2  
범위: GitHub-backed 개인 개발자 MVP  
비범위: 학교/부모/MDM, 금전 스테이크, GitLab/Bitbucket, enterprise admin

주의: 현재 구현 순서는 GitHub-first가 아니다. build-first 전략에 따라 Android/iOS 모바일 차단 가능성을 먼저 검증하고, 그 다음 GitHub scoring을 이 PRD 범위로 연결한다. 실행 순서는 [build-first-execution-plan.md](build-first-execution-plan.md), 기준 설계는 [app-design.md](app-design.md), 제품 전략/UX/사업 패키징은 [product-strategy-spec.md](product-strategy-spec.md)를 따른다.

## 1. 제품 목표

개발자가 GitHub에서 검증 가능한 개발 활동을 만들면, 그 활동을 스크린타임 크레딧으로 환산하고, 사용자가 선택한 방해 앱/웹사이트 접근을 크레딧 장부에 따라 열어준다.

핵심은 “차단 앱”이 아니라 `code-backed credit ledger`다.

## 2. 대상 사용자

1차 ICP:

- GitHub를 주로 쓰는 개인 개발자
- 사이드프로젝트/오픈소스 활동이 있다
- 스마트폰 방해 앱 사용을 줄이고 싶다
- 자기신고형 할 일 앱보다 자동 검증을 선호한다

2차 ICP:

- 취준생/학생 개발자
- ADHD/도파민 조절 니즈가 있는 개발자
- 부트캠프 수강생

## 3. 핵심 사용자 문제

개발자는 집중을 방해하는 앱을 줄이고 싶지만, 일반 차단 앱은 “왜 지금 열어주면 안 되는지/열어줘도 되는지”를 개발 워크플로와 연결하지 못한다. 자기신고형 체크리스트는 쉽게 속일 수 있고, 단순 타이머는 실제 산출물과 무관하다.

## 4. 가치 제안

사용자 관점:

- “오늘 실제로 개발한 만큼만 guilt-free leisure time을 쓴다.”
- “내가 왜 10분/25분/45분을 받았는지 설명을 본다.”
- “내가 고른 앱만 관리하고, 전체 폰을 빼앗기지 않는다.”

제품 관점:

- GitHub PR/commit/review/CI metadata로 개발 활동을 검증한다.
- 점수는 내부용이고, 사용자는 minutes와 explanation을 본다.
- 모바일 enforcement는 선택 앱/웹사이트 shield로 제한한다.
- proof ledger가 blocker 없이도 다시 볼 가치가 있을 때만 결제를 구현한다.

## 5. MVP 기능

### Must Have

| 기능 | 설명 |
| --- | --- |
| 계정 생성 | email/social login 중 하나 |
| GitHub App 연결 | repo allowlist, 최소 권한 |
| Webhook 수신 | pull_request, push, check_suite/check_run, pull_request_review |
| PR enrichment | files, commits, reviews, comments, status/checks |
| Feature extraction | diff size, file categories, test signal, CI, review, issue link |
| Rules-first scoring | provisional/confirmed credit 계산 |
| Credit ledger | earned, spent, clawed_back, override 기록 |
| Today screen | 오늘 획득/사용/잔여 크레딧 |
| Scoring explanation | 왜 몇 분을 받았는지 |
| App selection | 사용자가 막을 앱/웹사이트 선택 |
| Enforcement sync | 남은 credit에 따라 selected app shield/block |
| Override | 긴급 해제, 이유 입력, 빈도 제한 |
| Appeal | 판정 이의 제기, 재평가 요청 |
| Privacy controls | repo 연결 해제, data delete/export 최소 버전 |

### Should Have

- streak가 아닌 weekly rhythm view
- low-friction onboarding demo
- score simulator with past PRs
- suspicious activity flags
- daily cap 설정
- quiet hours

### Could Have

- WakaTime 연동
- browser extension blocker
- team/cohort preview
- custom scoring rules
- AI-generated explanation refinement

## 6. Non-Goals

- 휴대폰 전체 잠금
- 앱 삭제 방지
- 부모/학교 관리자 대시보드
- 미성년자용 앱
- 돈 예치/벌금
- 코드 품질 리뷰 자동화
- LLM 기반 full diff 심사
- leaderboard

## 7. 핵심 플로우

### Onboarding

최종 MVP onboarding:

1. 제품 약속 표시: 선택 앱/웹사이트를 관리한다.
2. GitHub 연결.
3. repo 선택.
4. 방해 앱/웹사이트 선택.
5. daily cap 설정.
6. 과거 PR 1-3개로 scoring demo 표시.
7. 정책 활성화.

현재 prototype onboarding:

1. 제품 약속 표시: 선택 앱만 로컬에서 관리한다.
2. Usage Access와 overlay 권한 안내.
3. 차단할 Android package 입력.
4. mock credit 설정.
5. monitor 활성화.

### Earn

1. 사용자가 GitHub에서 PR 생성/수정/merge.
2. webhook 수신.
3. queue에 event 저장.
4. PR enrichment.
5. feature vector 생성.
6. scoring decision 생성.
7. credit ledger에 적립.
8. mobile app이 credit sync.

### Spend

1. 사용자가 차단 대상 앱 접근.
2. credit 잔액 확인.
3. 잔액 있으면 접근 허용 및 spent ledger 기록.
4. 잔액 없으면 shield 표시.
5. shield에서 “최근 판정”과 “다음 unlock 조건” 표시.
6. 필요하면 override 또는 appeal.

## 8. Scoring Policy v0

### Credit Types

| 타입 | 설명 |
| --- | --- |
| provisional | merge 전 임시 보상. 낮은 cap과 expiry 적용 |
| confirmed | merge/CI/review 등 신뢰 신호로 확정 |
| bonus | review submitted, tests added 등 보너스 |
| clawback | revert/duplicate/spam 감지 시 회수 |
| manual_adjustment | appeal/admin/internal correction |

### Rules

기본:

- bot author 제외
- generated/vendor/lockfile 비중이 60% 이상이면 credit 상한 10분
- whitespace-only, rename-only는 0분 또는 5분 이하
- 같은 normalized patch hash 반복은 중복 차감
- PR merge 후 24시간 내 revert되면 clawback 후보
- daily confirmed cap 기본 60분
- daily provisional cap 기본 20분

### Solo Developer Fallback

개인 프로젝트는 리뷰가 없을 수 있다. 다음 중 2개 이상이면 commit batch도 낮은 provisional credit 가능:

- source file 변경
- test file 변경
- issue number 또는 structured commit message
- CI passed
- non-generated ratio 80% 이상
- diff가 너무 작거나 너무 크지 않음

## 9. Data Model

### Core Tables

| 테이블 | 주요 컬럼 |
| --- | --- |
| users | id, email, timezone, locale, tier |
| github_installations | user_id, installation_id, permissions, status |
| repositories | provider, external_id, owner, name, visibility, selected |
| inbound_events | provider, event_type, delivery_id, payload_hash, received_at, status |
| pull_requests | repo_id, number, author, state, merged_at, base_sha, head_sha |
| commit_batches | repo_id, author, from_sha, to_sha, committed_at |
| feature_vectors | subject_type, subject_id, features_json, extracted_at |
| score_decisions | subject_type, subject_id, score, credit_minutes, confidence, rationale_json |
| credit_ledger | user_id, delta_minutes, credit_type, source_id, reason, reversible, created_at |
| device_policies | user_id, platform, blocked_targets, daily_cap, strictness, updated_at |
| overrides | user_id, minutes, reason, created_at, expires_at |
| appeals | user_id, decision_id, note, status, resolution |
| risk_flags | user_id, flag_type, severity, evidence_json |

## 10. API v0

| API | Method | 설명 |
| --- | --- | --- |
| /auth/github/start | GET | GitHub App/OAuth 시작 |
| /auth/github/callback | GET | 인증 완료 |
| /webhooks/github | POST | GitHub webhook |
| /credits/today | GET | 오늘 크레딧 상태 |
| /activity/feed | GET | 최근 개발 활동과 scoring 결과 |
| /activity/:id | GET | scoring explanation |
| /policy | GET/PUT | 차단 정책 |
| /devices/register | POST | 모바일 디바이스 등록 |
| /devices/sync | POST | credit/policy sync |
| /override | POST | 긴급 해제 |
| /appeals | POST | 판정 이의 제기 |
| /privacy/export | POST | 데이터 내보내기 요청 |
| /privacy/delete | POST | 계정/데이터 삭제 요청 |

## 11. Analytics Events

| 이벤트 | 의미 |
| --- | --- |
| signup_started | 가입 시작 |
| github_connect_started | GitHub 연결 시작 |
| github_connected | GitHub 연결 완료 |
| repo_selected | repo allowlist |
| policy_enabled | 앱 차단 정책 활성화 |
| first_event_received | 첫 webhook 수신 |
| first_score_created | 첫 score decision |
| credit_earned | 크레딧 적립 |
| credit_spent | 크레딧 사용 |
| shield_shown | 차단 화면 표시 |
| override_requested | 긴급 해제 |
| appeal_submitted | 이의 제기 |
| uninstall_or_revoke | GitHub revoke 또는 앱 삭제 감지 |

## 12. Success Metrics

MVP alpha:

- GitHub connection rate: 60%+
- repo selection completion: 70%+
- first scored event within 7 days: 40%+
- D7 retention: 35%+
- D30 retention: 15%+
- weekly earned minutes median: 60분+
- appeal rate: scored events의 20% 이하
- override frequency: 사용자당 주 3회 이하
- voluntary enabled days: 사용자당 주 4일 이상

Prototype dogfood:

- monitor enabled days: 14일 중 8일 이상
- blocked attempts: 주 4회 이상
- override frequency: 주 3회 이하
- overlay show latency: 2초 이하
- natural scorable dev events: 14일 중 5개 이상

## 13. 주요 리스크

| 리스크 | 대응 |
| --- | --- |
| iOS Family Controls entitlement 지연 | Sprint 2 시작 시 신청, Android prototype 병행 |
| GitHub 연결 거부 | public-only mode, metadata-only mode, WakaTime fallback 검토 |
| PR-only가 너무 느림 | provisional commit batch 도입 |
| AI 판정 불신 | rules-first, explanation-first, appeal |
| 차단 우회 | lock mode는 플랫폼 정책 안에서만, override log와 cap 감소 |
| 악용 | generated/vendor/duplicate/revert/burst rules |
| 법무 리스크 | 미성년자/학교/부모/금전 스테이크 제외 |

## 14. 출시 판정

Private alpha로 넘어가기 전:

- Android 또는 iOS 중 하나에서 selected-app shield/block loop가 실제 기기에서 동작했다.
- 본인 실제 사용 7일 동안 매일 scoring/ledger/enforcement loop가 깨지지 않았다.
- scoring explanation의 appeal 또는 manual correction 대상이 score decision의 20% 이하로 유지됐다.
- 개인정보 처리 방침 초안과 repo data retention 정책이 준비됐다.
- “전체 폰 잠금”이 아니라 “선택 앱/웹사이트 관리”로 모든 카피가 정리됐다.
