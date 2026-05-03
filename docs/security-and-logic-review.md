# Security And Logic Review

문서 상태: v0.1
점검일: 2026-05-03
역할: 전체 로직, 계획, 정책, 보안 설계 점검 및 보완 기준

## 1. Executive Verdict

현재 설계는 `계속 개발` 가능하다. 다만 보안 관점의 결론은 명확하다.

1. Android local blocker는 dogfood용으로 충분하지만 tamper-proof 제품이 아니다.
2. GitHub scoring은 Sprint 4 전까지 다시 열면 안 된다. webhook 검증, dedupe, enrichment, ledger persistence가 같이 들어가야 한다.
3. 서버는 지금 health-only라 공격면이 작지만, 기본 설정은 local-only와 CORS allowlist로 닫아두는 편이 맞다.
4. 가장 큰 보안 리스크는 해킹보다 privacy overcollection이다. package name, quest title, repo metadata, PR diff는 모두 데이터 최소화 기준을 가져야 한다.
5. 정책 로직은 대체로 맞지만, Kotlin/TypeScript 이중 구현 drift를 막는 golden fixture test가 필요하다.

이번 점검에서 바로 반영한 보강:

- API 기본 bind host를 `127.0.0.1`로 바꾸고, `HOST=0.0.0.0`은 명시 설정일 때만 허용한다.
- API CORS 기본값을 open origin에서 disabled로 바꾸고, `ALLOWED_ORIGINS` allowlist를 추가한다.
- API config test를 추가했다.
- shared policy의 invalid timezone 입력이 서버 예외로 이어지지 않도록 UTC fallback을 추가했다.

## 2. Current System Surface

| 영역 | 현재 상태 | 보안 판단 |
| --- | --- | --- |
| Android app | local-only prototype | 권한/overlay/usage stats를 쓰므로 disclosure와 dogfood log 최소화가 중요 |
| Android storage | `MODE_PRIVATE` SharedPreferences, internal export file | prototype에는 적절. 민감 토큰 저장 전 암호화/Keystore 재검토 필요 |
| Android enforcement | UsageStats + foreground service + overlay | Play 정책상 AccessibilityService보다 안전한 선택 |
| Android app visibility | manual package input + recent foreground package | `QUERY_ALL_PACKAGES` 회피는 유지해야 함 |
| iOS skeleton | SwiftUI + FamilyControls/ManagedSettings skeleton | entitlement와 opaque token UX가 핵심 리스크 |
| API | health-only Fastify | 이제 local-only default. Sprint 4 전 auth/rate limit/webhook HMAC 필요 |
| Scoring | pure rule package only | privacy-safe scaffold. 실제 API 연동 전 raw diff 저장 금지 |
| Shared policy | TS canonical policy + Android mirror | drift 방지가 필요 |
| Dogfood export | user-initiated TSV share | package name/quest title이 포함되므로 export 전후 설명과 clear 기능 유지 |

## 3. Policy Logic Review

현재 정책 평가 순서는 맞다.

```text
own app / no foreground
-> target match
-> active weekday
-> active time
-> manual holiday
-> public holiday skip
-> free day
-> emergency unlock
-> credit available
-> credit empty
```

이 순서의 장점:

- 앱 자기 자신은 절대 막지 않는다.
- target이 아닌 앱은 정책 계산 대상이 아니다.
- 요일/시간/휴일은 credit보다 먼저 평가되어 사용자가 의도한 예외를 존중한다.
- free day와 emergency unlock은 credit spend를 만들지 않는다.
- credit이 있을 때만 spend engine이 동작한다.

보완 기준:

| 로직 | 현재 | 보완 |
| --- | --- | --- |
| timezone | Android와 TS 모두 invalid timezone을 UTC로 fallback | Sprint 4 API는 저장 시 IANA timezone allowlist/validation 추가 |
| active time | HH:mm 검증은 Android UI에서 함 | API policy write에도 같은 검증 필요 |
| weekday empty | empty면 inactive | UI에서 "선택된 요일 없음 = 차단 비활성"을 명확히 표시 |
| public holiday | 현재 placeholder false | 실제 공휴일 API 전까지 user-facing promise 금지 |
| emergency unlock | daily/weekly cap 있음 | 서버 sync 후 append-only ledger event로 기록 |
| strict mode | test shortcut 제한 | 삭제 방지/권한 회수 방지라는 의미로 확장 금지 |
| free day | mock proof로 local midnight까지 | GitHub proof 이후에는 reversible ledger source와 연결 |

## 4. Credit And Spend Logic Review

현재 Android spend rule은 `blocked target foreground + interactive + remainingMinutes > 0`일 때 60초마다 1분 차감한다.

유지할 invariant:

- `remainingMinutes >= 0`
- own package는 차단/차감하지 않음
- 권한 missing 상태에서는 overlay/spend 중단
- device not interactive 상태에서는 spend 중단
- free day/emergency/manual holiday/public holiday skip은 spend하지 않음
- 모든 manual/test credit mutation은 dogfood event에 기록

보완할 점:

1. Sprint 1.2 이후 `CreditStore`에 max local test cap을 둔다.
   예: debug prototype에서는 240분 이상 local mock credit 저장 금지.

2. Sprint 4 이후 local state는 cache가 되고 server ledger가 source of truth가 된다.
   모바일이 credit을 임의 증가시키는 API는 만들지 않는다.

3. Spend event는 ledger에서 `delta_minutes = -1`, `source = mobile_spend`, `target`, `policy_reason`을 갖는다.

4. clock manipulation 방어는 server sync 후 처리한다.
   local-only prototype은 사용자가 기기 시간을 바꾸면 우회 가능하다고 문서화한다.

## 5. Quest And Exception Logic Review

Daily Quest 방향은 맞다. todo click으로 unlock하지 않고, proof-backed completion만 free day로 이어진다.

현재 mock proof는 dogfood용이므로 제품에서 이렇게 말해야 한다.

```text
Mock proof는 prototype 검증용입니다.
실제 MVP에서는 commit, PR, review, CI 같은 proof event만 free day/credit에 연결됩니다.
```

보완:

- required quest가 0개일 때 free day를 주지 않는 현재 정책은 유지한다.
- quest title은 민감 정보가 될 수 있다. export/share 전에 로컬 로그에 포함된다는 안내가 필요하다.
- Sprint 4에서는 quest completion을 `proof_ref`와 묶고, proof가 clawback되면 quest/free day도 reversible이어야 한다.

Emergency Unlock 방향도 맞다. 사용자에게 우회 수단이 없으면 앱을 삭제하거나 권한을 꺼버릴 가능성이 높다.

보완:

- daily cap 3, weekly cap 10은 dogfood에서 조정한다.
- strict mode에서 30분 emergency unlock 제한은 유지한다.
- emergency reason은 free text라서 서버 sync 전에는 private/local로만 둔다.

## 6. Android Security Review

현재 Android 선택은 정책 친화적이다.

유지해야 할 것:

- AccessibilityService 사용 금지.
- Device Admin/Device Owner 사용 금지.
- 앱 삭제 방지 금지.
- `QUERY_ALL_PACKAGES` 사용 금지.
- 전체 휴대폰 잠금 약속 금지.
- Usage Access와 Overlay 권한은 사용자가 직접 켜고 끌 수 있게 둔다.

현재 안전장치:

- `android:allowBackup="false"`
- `MonitorService`는 `exported=false`
- `MainActivity`만 launcher로 exported
- local storage는 `MODE_PRIVATE`
- export는 user-initiated share sheet
- overlay test-credit shortcut은 strict mode에서 숨김

보완:

| 우선순위 | 보완 | 이유 |
| --- | --- | --- |
| P0 before Play/Internal test | in-app prominent permission disclosure | Usage Access/Overlay 목적과 저장 데이터를 명시 |
| P0 before Play/Internal test | privacy screen | dogfood log/package name/quest title 저장과 clear/export 설명 |
| P1 | package name validation | 잘못된 입력과 자기 package 저장 방지 UX 개선 |
| P1 | event redaction option | export에서 target package/quest title을 숨기는 옵션 |
| P1 | foreground detection failure state | 제조사/OS별 UsageStats 불안정성을 사용자에게 설명 |
| P2 | encrypted preferences review | GitHub token 등 민감 토큰 저장 전 Android Keystore 기반 저장 검토 |

## 7. iOS Security Review

iOS는 아직 skeleton이므로 구현 전 보안 기준을 고정한다.

유지해야 할 것:

- FamilyControls authorization을 먼저 받는다.
- FamilyActivityPicker로만 target을 선택한다.
- selected app/domain은 opaque token으로 다룬다.
- 앱 내부에서 iOS target app name 표시를 약속하지 않는다.
- ManagedSettings shield는 `remainingMinutes <= 0`일 때만 적용한다.
- DeviceActivity spend는 실제 기기 검증 전까지 구현하지 않는다.

보완:

- App Group container에 저장할 값 목록을 `selection token reference`, `credit cache`, `policy version`으로 제한한다.
- ShieldAction extension은 emergency unlock을 직접 허용하지 않는다. main app 또는 server policy를 통해서만 해제한다.
- entitlement 승인/거절/취소 상태를 UI에 표시한다.
- FamilyControls individual authorization은 부모/학교 통제와 다르다는 copy를 명확히 한다.

## 8. API And Backend Security Review

현재 API는 `/health`만 제공한다. 이번 점검에서 local-only default로 줄였다.

현재 기준:

| 설정 | 기본값 | 이유 |
| --- | --- | --- |
| `HOST` | `127.0.0.1` | local prototype을 외부 네트워크에 열지 않음 |
| `PORT` | `4000` | 명시적이고 검증된 포트 |
| `ALLOWED_ORIGINS` | empty | CORS disabled by default |
| bodyLimit | 5MB | GitHub webhook payload 상한을 염두에 둔 임시 제한 |
| rawBody | preserved | Sprint 4 webhook HMAC 검증 준비 |

Sprint 4 전에 반드시 추가:

1. GitHub webhook signature verification
   `X-Hub-Signature-256` HMAC SHA-256을 raw body와 secret으로 검증하고 timing-safe compare를 쓴다.

2. Delivery dedupe
   `X-GitHub-Delivery`를 unique key로 저장한다.

3. Event allowlist
   `pull_request`, `pull_request_review`, `check_run/check_suite`, `push` 등 필요한 event만 받는다.

4. Installation/repo allowlist
   사용자가 연결한 installation/repo만 enrichment한다.

5. Rate limit and replay handling
   signature pass + delivery id 중복 + timestamp/audit status를 저장한다.

6. Auth
   mobile/user API는 session/JWT/OAuth token 검증 전까지 만들지 않는다.

7. CORS
   production web origin만 `ALLOWED_ORIGINS`에 넣는다. wildcard 금지.

## 9. GitHub Scoring Security Review

현재 scoring package는 pure function이라 안전하다. 문제는 Sprint 4에서 외부 데이터를 붙일 때 생긴다.

필수 설계:

| 영역 | 기준 |
| --- | --- |
| GitHub App permissions | Contents/Metadata/Pull requests/Checks 등 최소 권한 |
| private repo raw diff | 기본 저장 금지 |
| LLM input | structured features 우선, 최소 hunk만 opt-in |
| duplicate patch | patch hash 저장하되 raw patch 장기 보관 금지 |
| bot/automation | bot author, CI account 제외 |
| idempotency | subject provider/id/sha/event type 기반 unique decision |
| clawback | revert/duplicate/rebase anomaly는 reversible ledger로 처리 |
| explanation | score보다 minutes/reasons/risk flags만 사용자에게 노출 |

현재 보완할 scoring gap:

- GitHub API가 patch를 생략하는 큰 diff에서 `whitespaceOnly=false`로 처리된다. Sprint 4에서는 `patch_missing` risk flag를 추가해야 한다.
- `ciPassed`는 boolean 하나로 충분하지 않다. check conclusion, required check 여부, stale check 여부를 분리해야 한다.
- approvals는 self-review/bot-review를 제외해야 한다.
- docs-only credit cap은 유지하되 developer docs 작업을 완전히 무가치하게 만들지는 않는다.

## 10. Data Classification

| 데이터 | 예 | 민감도 | 저장 기준 |
| --- | --- | --- | --- |
| Local policy | blocked package, strictMode, schedule | medium | local-only, clear 가능 |
| Dogfood event | package, reason, quest title | medium | 최근 1,000개, user export only |
| Credit ledger | earned/spent/override | medium | Sprint 4부터 server source of truth |
| GitHub metadata | repo, PR number, author, timestamps | medium/high for private repo | 최소 저장, user revoke/delete 필요 |
| Raw diff | code hunk, file path | high | 기본 장기 저장 금지 |
| OAuth token | GitHub installation/user token | high | server secret store only, mobile 저장 금지 |
| LLM prompt/output | diff summary/reasons | high if private repo | opt-in/private repo masking 필요 |

## 11. Abuse And Bypass Model

사용자 자신이 우회하려는 경우:

- 앱 삭제, 권한 회수, overlay 권한 끄기, 기기 시간 변경, mock credit 추가.
- MVP 대응은 tamper-proof가 아니라 detection/explanation이다.
- dogfood에서는 `permission_missing`, `monitor_stopped`, `manual_credit_changes`, `override`를 관찰한다.

외부 공격자:

- GitHub webhook spoofing/replay.
- API auth bypass.
- CORS abuse.
- token leakage.
- private diff overcollection.

MVP 전 대응:

- webhook HMAC + dedupe
- CORS allowlist
- auth before user APIs
- secrets server-only
- raw diff retention off
- audit log append-only

## 12. Test Gaps

추가해야 할 테스트:

| 우선순위 | 테스트 | 이유 |
| --- | --- | --- |
| P0 | TS/Kotlin policy golden fixtures | 정책 drift 방지 |
| P0 | API config/env tests | local-only/security default 유지 |
| P0 | GitHub webhook signature tests | Sprint 4 entry gate |
| P1 | DogfoodEventStore unit tests | export/parse/sanitize/max event 보장 |
| P1 | Android spend accumulator tests | foreground switch/interactive false/credit depletion 검증 |
| P1 | scoring patch_missing/CI required/self-review cases | scoring abuse 방지 |
| P2 | iOS shield state unit/UI tests | Xcode 프로젝트 후 가능 |

이번 점검에서 API config test와 invalid timezone policy test는 추가했다.

## 13. Updated Gates

### Gate A: Android Enforcement Viability

기존 유지. 추가 조건:

- permission disclosure 문구가 UI에 있어야 한다.
- 권한 회수/monitor stopped가 dogfood summary에 보여야 한다.

### Gate B: Dogfood Need

기존 유지. 추가 조건:

- 14일 동안 manual credit add가 대부분이면 실제 proof ledger 전환 전 UX가 약하다는 신호다.
- override가 많은 날에는 이유를 정리한다.

### Gate C: Developer Proof Supply

기존 유지. 추가 조건:

- GitHub PR-only가 부족하면 WakaTime/IDE/commit batch fallback을 설계한다.
- 단, fallback도 자기신고 checkbox가 아니라 machine-observed proof여야 한다.

### Gate D: Trust And Privacy

신규 gate로 승격한다.

통과 조건:

- Android privacy/permission screen 존재.
- GitHub data retention draft 존재. 상세 기준은 [github-sprint4-entry.md](github-sprint4-entry.md)를 따른다.
- private repo raw diff 기본 저장 금지.
- user revoke/delete path 설계.
- webhook HMAC/dedupe 구현 전에는 GitHub sync 금지.

### Gate E: Monetization

기존 유지. 추가 조건:

- paid plan 전에는 ledger가 blocker 없이도 가치 있는지 확인한다.
- money stake는 계속 금지.

## 14. Stop List

아래는 보안/정책상 계속 금지한다.

- AccessibilityService 기반 자동 조작
- `QUERY_ALL_PACKAGES`
- Device Admin으로 삭제 방지
- 전체 기기 잠금 약속
- private repo full diff 장기 저장
- LLM에 private diff 기본 전송
- wildcard CORS
- 모바일 앱에 GitHub OAuth secret 저장
- 결제/벌금/stake
- 부모/학교/MDM 모드

## 15. Next PRs After This Review

Completed after this review:

- `docs/dogfood-runbook`
- `test/android-event-store`
- `test/policy-golden-fixtures`
- `feature/android-privacy-permissions`
- `feature/android-dogfood-review`
- `refactor/android-main-sections`
- `docs/github-sprint4-entry`

Continue in this order:

1. Android real-device dogfood smoke
   권한, overlay, local log/export, Gate A/D copy가 실제 기기에서 납득되는지 확인한다.

2. `feature/github-webhook-security`
   [github-sprint4-entry.md](github-sprint4-entry.md)의 PR A 기준으로 signature verification, delivery dedupe, inbound event tests를 구현한다.

## 16. Sources Checked

- GitHub webhook validation: https://docs.github.com/en/webhooks/using-webhooks/validating-webhook-deliveries
- GitHub webhook events and payloads: https://docs.github.com/en/webhooks/webhook-events-and-payloads
- Android UsageStatsManager: https://developer.android.com/reference/android/app/usage/UsageStatsManager
- Android app security best practices: https://developer.android.com/privacy-and-security/security-best-practices
- Google Play AccessibilityService API policy: https://support.google.com/googleplay/android-developer/answer/10964491
- Google Play QUERY_ALL_PACKAGES policy: https://support.google.com/googleplay/android-developer/answer/10158779
- Google Play sensitive permissions/API policy: https://support.google.com/googleplay/android-developer/answer/9888170
- Apple FamilyControls: https://developer.apple.com/documentation/familycontrols
- Apple ManagedSettings shield: https://developer.apple.com/documentation/managedsettings/managedsettingsstore/shield
- Apple Managed Settings privacy model: https://developer.apple.com/documentation/managedsettings/connectionwithframeworks
- OWASP MASVS: https://mas.owasp.org/MASVS/
- OWASP ASVS: https://owasp.org/www-project-application-security-verification-standard/
