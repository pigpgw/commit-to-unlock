# Android Dogfood Runbook

문서 상태: v0.1
작성일: 2026-05-03
역할: Android local blocker MVP-A를 실제 기기에서 반복 검증하는 실행 절차

## 1. Goal

이 runbook의 목표는 기능 시연이 아니라 14일 동안 계속 켜둘 만한 차단 루프인지 판단하는 것이다.

검증 질문:

1. UsageStats 기반 foreground 감지가 실기기에서 충분히 안정적인가?
2. 선택한 target app에서 credit `0`일 때 overlay가 빠르게 뜨는가?
3. credit, free day, manual holiday, emergency unlock 우선순위가 실제 사용 중 납득되는가?
4. 사용자가 권한을 끄거나 앱을 삭제하고 싶어질 만큼 답답한가?
5. GitHub scoring 없이도 proof/quest/credit 개념이 자연스럽게 느껴지는가?

이번 runbook으로 판단하지 않는 것:

- 결제 가능성
- GitHub PR scoring 정확도
- iOS Family Controls 구현 가능성
- 부모/학교/MDM 모델
- tamper-proof enforcement

## 2. Test Window

기본 실험 기간은 14일이다.

최소 유효 데이터:

| 항목 | 기준 |
| --- | --- |
| monitor enabled days | 8일 이상 |
| blocked attempts | 8회 이상 |
| mock proof completions | 5회 이상 |
| emergency unlocks | 14일 동안 6회 이하 |
| exported TSV | 매 dogfood day마다 1개 이상 |

14일 전이라도 Gate A가 명확히 실패하면 멈추고 Android 차단 방식부터 고친다.

## 3. Preconditions

필수:

- 물리 Android 기기 1대
- USB debugging enabled
- Android platform-tools `adb`
- JDK 17
- Android SDK 33
- Node.js 22+
- pnpm 10+

권장:

- 개인이 실제로 자주 여는 방해 앱 2-4개
- 테스트 중 삭제하지 않을 수 있는 보조 브라우저 또는 SNS 앱
- 하루 1회 같은 시간대에 export/analyze하는 루틴

여러 기기가 연결되어 있으면 항상 `ANDROID_SERIAL=<device-id>`를 붙인다.

## 4. Data Handling

Dogfood export는 local-only TSV다. 서버로 업로드하지 않는다.

TSV에 들어갈 수 있는 데이터:

- target package name
- policy reason
- remaining credit
- quest title 또는 emergency reason 일부
- event timestamp

보관 규칙:

| 데이터 | 규칙 |
| --- | --- |
| local device events | 앱 내부 최근 1,000개 event |
| local export file | debug app internal storage의 `dogfood-export.tsv` |
| repo artifact | `artifacts/android-dogfood/`, git ignore 유지 |
| 공유 | 사용자가 명시적으로 share/export할 때만 |
| 삭제 | 앱 내 clear 기능 또는 앱 데이터 삭제 |

금지:

- export TSV를 커밋하지 않는다.
- 민감한 quest title을 장기 보관하지 않는다.
- 패키지명 데이터를 사용자 동의 없이 서버로 보내지 않는다.

## 5. One-Time Setup

1. 최신 `dev` 기준 작업물을 설치한다.

   ```bash
   pnpm android:dogfood
   ```

2. 앱 첫 화면에서 개발자 gate를 통과한다.

   기대:

   - `예` 선택 시 prototype 화면 진입
   - `아니오` 선택 시 playful rejection 후 종료

3. Android 권한을 켠다.

   필수:

   - Usage Access
   - Display over other apps
   - Notifications on Android 13+

4. Commit Unlock으로 돌아와 `Refresh status`를 누른다.

   기대:

   - `Usage Access: granted`
   - `Overlay Permission: granted`
   - `Monitor service: stopped`

5. target package를 등록한다.

   권장 시작값:

   ```text
   com.android.chrome
   ```

   실제 dogfood에서는 개인이 줄이고 싶은 앱을 2-4개까지 등록한다.

6. policy schedule을 저장한다.

   기본값:

   - weekdays: Monday-Friday
   - active time: blank for all day
   - manual holiday: off
   - public holiday skip: off until real holiday source exists

## 6. Smoke Checklist

처음 설치하거나 주요 변경 후 이 smoke를 반드시 수행한다.

| 단계 | 조작 | 기대 결과 |
| --- | --- | --- |
| 1 | credit을 `0`으로 reset | remaining credit이 `0` |
| 2 | monitor start | foreground 감지 시작 |
| 3 | target app 열기 | overlay가 2초 안팎으로 표시 |
| 4 | overlay 확인 | target, reason `credit_empty`, remaining credit, strict mode 표시 |
| 5 | Commit Unlock 복귀 후 test credit 추가 | remaining credit `> 0` |
| 6 | target app 다시 열기 | overlay가 유지되지 않음 |
| 7 | target app 60초 foreground 유지 | credit 1분 차감 |
| 8 | required quest 추가 | 아직 free day 아님 |
| 9 | mock proof completion | `free_day` 적용 |
| 10 | emergency unlock 5분 | credit `0`이어도 target 허용 |
| 11 | strict mode 켜기 | overlay test-credit shortcut 숨김 |

Smoke 실패 기준:

- Usage Access granted인데 foreground package가 계속 비어 있음
- target foreground 이후 overlay가 반복적으로 5초 이상 늦음
- credit `> 0`인데 overlay가 계속 남음
- own app이 차단됨
- emergency/free day/manual holiday 상태에서 credit이 차감됨

## 7. Daily Dogfood Loop

매 dogfood day에 아래 순서로 실행한다.

1. 앱을 열고 권한/monitor 상태를 확인한다.
2. monitor를 켠다.
3. 평소처럼 target app을 사용하려고 시도한다.
4. 막히면 다음 중 하나를 실제 의도대로 선택한다.

   - 그냥 닫고 개발로 돌아간다.
   - test credit을 추가한다.
   - required quest를 mock proof로 완료한다.
   - emergency unlock을 사용한다.
   - manual holiday/free day를 켠다.

5. 하루가 끝나면 export한다.

   ```bash
   pnpm android:dogfood:export
   ```

6. export 결과를 분석한다.

   ```bash
   pnpm android:dogfood:analyze
   ```

7. analyzer 결과에서 Gate Snapshot, Data Quality, Recommendations를 확인한다.
   앱 내부의 `Dogfood review` 섹션도 함께 확인해 export 전후의 Gate A/B/C/D 상태가 예상과 맞는지 본다.

일일 메모에 남길 것:

| 항목 | 예시 |
| --- | --- |
| dogfood date | `2026-05-03` |
| device/OS | `Pixel 7 / Android 15` |
| target apps | `Chrome, YouTube` |
| blocker felt useful? | `yes/no/mixed` |
| most annoying issue | `overlay delay`, `permission prompt`, `too strict`, `none` |
| emergency reason pattern | `work message`, `navigation`, `unknown` |
| product decision note | `strict mode ok`, `need weekend default`, `target setup confusing` |

## 8. Export Naming And Archive Rule

기본 스크립트는 UTC timestamp 파일명을 만든다.

```text
artifacts/android-dogfood/dogfood-export-YYYYMMDDTHHMMSSZ.tsv
```

수동 파일명을 쓸 때는 아래 형식을 쓴다.

```text
artifacts/android-dogfood/YYYY-MM-DD_<device>_<session>.tsv
```

예:

```bash
pnpm android:dogfood:export artifacts/android-dogfood/2026-05-03_pixel7_evening.tsv
```

Archive rule:

- raw TSV는 git에 커밋하지 않는다.
- 의사결정에 쓸 요약은 문서에 숫자만 옮긴다.
- export에 민감한 quest/emergency reason이 있으면 공유 전에 제거한다.

## 9. Gate Decision Template

14일이 끝나면 아래 템플릿으로 판단한다.

```markdown
# Dogfood Gate Decision

Window:
- start:
- end:
- device / OS:
- target apps:

Analyzer summary:
- active days:
- monitor enabled days:
- blocked attempts:
- policy blocks:
- permission failures:
- overlay shows:
- automatic credit spends:
- emergency unlocks:
- free days:
- daily quest adds:
- daily quest mock completions:

Data quality:
- target coverage:
- policy reason coverage:
- credit remaining coverage:

Gate A - Enforcement Viability:
- status: pass / fail / needs_data
- evidence:
- decision:

Gate B - Dogfood Need:
- status: pass / fail / needs_data
- evidence:
- decision:

Gate C - Developer Proof Supply:
- status: pass / fail / needs_data
- evidence:
- decision:

Gate D - Trust And Privacy:
- status: pass / fail / needs_data
- permission copy was clear:
- export/clear behavior was clear:
- local-only/tamper-proof limitation was clear:
- privacy blockers before GitHub sync:

Next action:
- continue Android local blocker / improve Android permissions / widen proof sources / start Sprint 4 spec / pivot
```

## 10. Gate Rules

### Gate A: Enforcement Viability

Pass when:

- foreground events exist
- overlay events or blocked attempts exist
- target app block happens within roughly 2 seconds on normal use
- own app is never blocked
- policy exceptions do not spend credit

Fail when:

- UsageStats cannot reliably detect foreground apps on the main test device
- overlay is consistently late or suppressed
- permission recovery is confusing enough that the app would be deleted

Decision:

- pass: continue to data quality and privacy work
- fail: fix Android enforcement before adding GitHub scoring

### Gate B: Dogfood Need

Pass when:

- monitor enabled days >= 8 in 14 days
- blocked attempts >= 8 in 14 days
- emergency unlocks <= 6 in 14 days
- user does not repeatedly disable permissions

Fail when:

- there are too few natural blocked attempts
- emergency unlock dominates actual usage
- user mostly turns off monitor or permissions

Decision:

- pass: blocker loop is worth continuing
- fail: consider desktop/browser-first proof ledger before mobile enforcement

### Gate C: Developer Proof Supply

Pass when:

- mock proof completions >= 5 in 14 days
- user can explain what real proof would replace mock proof
- daily quest completion feels less like todo clicking and more like proof-backed unlock

Fail when:

- proof completions are rare
- PR-only proof would be too sparse
- user mostly needs IDE/WakaTime/local git signals instead of GitHub PRs

Decision:

- pass: prepare Sprint 4 GitHub entry spec
- fail: widen proof sources before GitHub-only scoring

### Gate D: Trust And Privacy

Pass when:

- Usage Access, Overlay, Notification purpose is clear before or during setup
- app states that this is local-only and not tamper-proof
- export and clear behavior are obvious
- package name/quest title retention is explained

Fail when:

- user cannot tell what data is stored
- permission prompts feel like dark patterns
- export contains surprising private text
- app implies uninstall prevention or full phone lock

Decision:

- pass: privacy copy is good enough for local dogfood
- fail: build privacy/permission screen before more product work

## 11. Troubleshooting

### No device

```bash
adb devices -l
```

If no device appears:

- enable USB debugging
- reconnect cable
- accept device trust prompt
- set `ANDROID_SERIAL` when multiple devices are connected

### Overlay does not show

Check:

- Usage Access granted
- Display over other apps granted
- monitor service running
- package name exactly matches foreground package
- credit is `0`
- policy day/time is active
- manual holiday/free day/emergency unlock is off

### Credit does not spend

Check:

- target app stayed foreground for 60 seconds
- device screen stayed interactive
- policy reason is `credit_available`
- free day or emergency unlock is not active

### Analyzer says needs_data

This is expected before enough sessions. Do not treat early `needs_data` as failure unless Gate A smoke fails repeatedly.

## 12. Next PRs After This Runbook

Completed after this runbook:

- `test/android-event-store`
- `test/policy-golden-fixtures`
- `feature/android-privacy-permissions`
- `feature/android-dogfood-review`
- `refactor/android-main-sections`
- `docs/github-sprint4-entry`

Continue in this order:

1. Real-device smoke and 14-day dogfood collection
2. Sprint 4 PR A only after Gate A/D evidence

Do not resume GitHub scoring until Gate A/B/D have enough evidence and the Sprint 4 entry spec is complete.
