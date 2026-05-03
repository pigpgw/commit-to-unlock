# GitHub Sprint 4 Entry Spec

문서 상태: v0.1
작성일: 2026-05-03
역할: GitHub proof/scoring/API sync를 다시 열기 전 필요한 보안, 권한, 데이터, 테스트 기준

## 1. Decision

Sprint 4는 바로 "GitHub scoring 구현"으로 시작하지 않는다. 먼저 아래 순서로 들어간다.

```text
webhook security -> delivery dedupe -> PR enrichment -> feature vector -> score decision -> credit ledger -> mobile sync
```

허용:

- GitHub App 기반 설치와 installation token 사용
- webhook-first ingestion
- PR/MR가 아닌 GitHub PR 중심 proof
- rules-first scoring
- append-only credit ledger
- metadata/feature vector 중심 저장

금지:

- signature 검증 없는 webhook route
- delivery dedupe 없는 ledger write
- PR files/reviews/checks enrichment 없는 scoring
- private repo raw diff 장기 저장
- 모바일 앱에 GitHub client secret/private key 저장
- LLM에 private diff 기본 전송
- GitHub sync를 Android local mock credit과 바로 섞기

## 2. Official Docs Checked

2026-05-03 기준 공식 GitHub 문서에서 확인한 기준:

| 주제 | 적용 기준 | 공식 문서 |
| --- | --- | --- |
| Webhook signature | `X-Hub-Signature-256` HMAC-SHA256, raw body, timing-safe compare | [Validating webhook deliveries](https://docs.github.com/en/webhooks/using-webhooks/validating-webhook-deliveries) |
| Webhook troubleshooting | secret 없으면 signature header가 없고, legacy SHA1 header는 사용하지 않음 | [Troubleshooting webhooks](https://docs.github.com/en/webhooks/testing-and-troubleshooting-webhooks/troubleshooting-webhooks) |
| GitHub App permissions | GitHub App은 기본 권한이 없고 최소 권한만 선택 | [Choosing permissions for a GitHub App](https://docs.github.com/developers/apps/building-github-apps/setting-permissions-for-github-apps) |
| Endpoint permissions | REST endpoint별 GitHub App permission 확인 필요 | [Permissions required for GitHub Apps](https://docs.github.com/en/rest/authentication/permissions-required-for-github-apps) |
| PR enrichment | PR files, commits, reviews, review comments를 REST API로 조회 가능 | [Pull requests REST API](https://docs.github.com/en/rest/pulls/pulls) |
| Reviews | review state, author, submitted_at, comments 조회 가능 | [Pull request reviews REST API](https://docs.github.com/en/rest/pulls/reviews) |
| Checks | check runs/suites는 GitHub App 중심 API로 조회 | [Checks REST API](https://docs.github.com/en/rest/checks) |
| Events | GitHub App webhook payload에는 installation/repository/sender context가 포함될 수 있음 | [Webhook events and payloads](https://docs.github.com/en/webhooks/webhook-events-and-payloads) |

## 3. Sprint 4 Entry Gates

Sprint 4 runtime 구현은 아래 gate를 모두 만족할 때만 시작한다.

| Gate | 기준 | 상태 |
| --- | --- | --- |
| A | Android selected-app enforcement smoke가 최소 1회 실제 기기에서 통과 | pending device evidence |
| B | 14일 dogfood need 판단 자료가 있거나, GitHub spec만 먼저 구현할 명시적 이유가 문서화됨 | pending |
| C | PR-only proof가 너무 드물면 commit batch/WakaTime/IDE fallback 여부가 결정됨 | pending |
| D | 이 문서의 HMAC/dedupe/retention/revoke/delete 기준이 확정됨 | this spec |

현재 결정:

- 이 문서는 Gate D 설계 산출물이다.
- 실제 webhook/scoring route는 Gate A/D smoke evidence와 이 문서 기준의 테스트가 준비된 뒤 구현한다.

## 4. GitHub App Model

### 4.1 App Type

MVP는 OAuth App이 아니라 GitHub App을 우선한다.

이유:

- repository 단위 설치와 repo allowlist가 가능하다.
- installation access token을 server-side로 발급할 수 있다.
- webhook event와 permission을 설치 범위에 묶을 수 있다.
- 사용자 계정 password/token을 모바일에 저장하지 않아도 된다.

### 4.2 Secret Storage

| Secret | 저장 위치 | 모바일 저장 | 비고 |
| --- | --- | --- | --- |
| GitHub App private key | server secret store | no | installation token 발급용 |
| webhook secret | server secret store | no | HMAC 검증용 |
| installation access token | server memory/cache | no | 짧은 수명. DB 장기 저장 금지 |
| user session token | server-issued auth | yes, secure storage later | Sprint 4 mobile sync 전까지 deferred |

Android local prototype은 GitHub token을 저장하지 않는다.

## 5. Minimum Permissions

초기 권한은 read-only 중심으로 시작한다.

| Permission | Access | 필요 이유 | Sprint 4 사용 |
| --- | --- | --- | --- |
| Metadata | read | repo identity, installation/repo context | required |
| Pull requests | read | PR files, commits, reviews, review comments | required |
| Checks | read | check runs/suites, CI conclusion | required |
| Commit statuses | read | checks가 없는 repo의 legacy status fallback | optional |
| Contents | read | compare/blob/file content가 꼭 필요할 때만 | avoid by default |
| Issues | read | issue link/comment enrichment | defer |
| Webhooks | read/write | repo webhook 관리 | not needed for GitHub App-level webhook |

권한 원칙:

- `Contents: read`는 첫 등록에서 피한다. PR files endpoint의 metadata로 충분한지 먼저 검증한다.
- raw file content가 필요한 기능은 Sprint 4가 아니라 별도 privacy review 후 연다.
- `Issues: read`는 PR body/branch/ticket reference로 부족할 때만 추가한다.
- 권한 추가가 필요하면 `decision-log.md`에 이유를 남긴다.

## 6. Webhook Events

초기 subscribe:

| Event | Actions | 목적 |
| --- | --- | --- |
| `installation` | created, deleted, suspend, unsuspend | 설치/revoke 상태 관리 |
| `installation_repositories` | added, removed | repo allowlist 변경 |
| `pull_request` | opened, synchronize, reopened, closed | PR lifecycle, merged credit trigger |
| `pull_request_review` | submitted, edited, dismissed | approvals/change requests/review signal |
| `pull_request_review_comment` | created, edited, deleted | review discussion signal |
| `check_run` | completed, rerequested | CI conclusion |
| `check_suite` | completed, rerequested | CI suite conclusion fallback |

Deferred:

| Event | 이유 |
| --- | --- |
| `push` | commit batch provisional credit를 열 때만 추가한다. 먼저 PR 중심으로 악용 면적을 줄인다. |
| `issue_comment` | PR issue comments가 필요할 수 있지만 noise가 크다. issue linkage는 PR body/branch/ref parsing으로 먼저 처리한다. |
| `status` | legacy CI가 많은 repo에서만 `Commit statuses: read`와 함께 추가한다. |

## 7. Webhook Receiver Contract

Endpoint:

```text
POST /webhooks/github
```

Required headers:

| Header | 사용 |
| --- | --- |
| `X-Hub-Signature-256` | raw body HMAC 검증 |
| `X-GitHub-Delivery` | idempotency key |
| `X-GitHub-Event` | event allowlist |
| `X-GitHub-Hook-ID` | audit/debug |

Processing order:

```text
1. read raw body
2. require signature header
3. verify HMAC-SHA256 with timing-safe compare
4. parse JSON only after signature pass
5. require event allowlist
6. require delivery id
7. insert inbound_events with unique provider + delivery_id
8. if duplicate, return 202 without side effects
9. enqueue enrichment job
10. return 202
```

Failure responses:

| Failure | Status | 저장 |
| --- | ---: | --- |
| missing signature | 401 | audit only, no payload parse |
| invalid signature | 401 | audit only, no payload parse |
| unknown event | 202 | inbound event with `ignored_event` if signature passed |
| duplicate delivery | 202 | no new job, no ledger write |
| invalid JSON after signature | 400 | inbound event with parse error |
| temporary enqueue failure | 503 | inbound event `verified_but_not_enqueued` |

Test vector:

- Add a unit test using GitHub's official sample secret/payload/signature from the webhook validation docs.
- Add a mutation test where the same payload with one changed byte fails verification.
- Add a duplicate delivery test proving ledger rows are not duplicated.

## 8. Delivery Dedupe And Idempotency

### 8.1 Inbound Event Dedupe

Unique key:

```text
provider = "github"
delivery_id = X-GitHub-Delivery
```

Table shape:

| Column | Type | Rule |
| --- | --- | --- |
| id | uuid | primary key |
| provider | text | `github` |
| delivery_id | text | unique with provider |
| event_type | text | header value |
| action | text nullable | payload action |
| installation_id | text nullable | payload installation id |
| repository_id | text nullable | GitHub repo id |
| payload_hash | text | SHA-256 of raw body |
| signature_verified_at | timestamp nullable | set only after pass |
| received_at | timestamp | server time |
| status | text | received, verified, ignored, enqueued, processed, failed |
| error_code | text nullable | stable error code |
| raw_payload_expires_at | timestamp nullable | short retention only |

Raw payload rule:

- Store raw payload only for short debug retention in non-production, or encrypted short retention in production.
- Default long-term storage is `payload_hash` + normalized event fields.
- Private repo PR diff/body text must not be copied into long-term raw payload tables.

### 8.2 Subject Idempotency

Scoring must also be idempotent by subject.

Unique key examples:

```text
score_decisions: provider + subject_type + subject_id + head_sha + rule_version
credit_ledger: user_id + source_type + source_id + ledger_event_type
proof_events: provider + proof_type + external_id + action + observed_sha
```

Rules:

- Replayed webhook can update processing status but cannot add credit twice.
- New `synchronize` on same PR creates a new feature vector only if `head_sha` changed.
- `closed` with `merged=true` can confirm previous provisional credit or create confirmed credit once.
- `closed` with `merged=false` does not create confirmed credit.
- Revert/clawback uses a new negative ledger row; it does not mutate old rows.

## 9. PR Enrichment

### 9.1 Enrichment Inputs

For a `pull_request` subject, collect:

| Data | Endpoint/source | Store raw? | Store normalized? |
| --- | --- | --- | --- |
| PR metadata | webhook + `GET /pulls/{number}` | no | yes |
| PR files | `GET /pulls/{number}/files` | no patch retention | file path/status/additions/deletions/changes |
| PR commits | `GET /pulls/{number}/commits` | no | sha, author, timestamp |
| PR reviews | `GET /pulls/{number}/reviews` | no body retention | state, reviewer id, submitted_at |
| Review comments | review comments endpoint | no body retention | count, author, path, created_at |
| Checks | check runs/suites for head sha | no logs | name, conclusion, completed_at |

Do not store:

- full raw diff
- patch hunks
- file contents
- review body text
- PR body text beyond derived issue-link/ticket-link flags
- LLM prompts containing private repo content

Allowed derived fields:

- `changed_files`
- `additions`
- `deletions`
- `source_files`
- `test_files`
- `docs_files`
- `config_files`
- `lock_or_vendor_files`
- `generated_ratio`
- `issue_linked`
- `ci_present`
- `ci_passed`
- `approvals`
- `change_requests`
- `review_comments`
- `discussion_paths_count`
- `patch_missing_count`
- `bot_or_self_review_count`

### 9.2 File Classification

Feature extraction should classify by path/extension first.

| Class | Examples |
| --- | --- |
| source | `.ts`, `.tsx`, `.kt`, `.swift`, `.py`, `.go`, `.java`, `.rb` |
| test | `*.test.*`, `*.spec.*`, `__tests__`, `src/test`, `androidTest` |
| docs | `.md`, `docs/`, `README`, `CHANGELOG` |
| config | `.yml`, `.yaml`, `.json`, `.toml`, `.gradle`, `Dockerfile` |
| lock/vendor/generated | lockfiles, `vendor/`, `dist/`, generated markers |

Risk flags:

- `patch_missing`
- `lockfile_only`
- `docs_only`
- `generated_or_vendor_heavy`
- `whitespace_unknown`
- `self_review_only`
- `bot_author`
- `ci_missing`
- `ci_failed`
- `duplicate_patch_risk`

## 10. Scoring And Ledger

Sprint 4 scoring flow:

```text
proof_event -> feature_vector -> score_decision -> credit_ledger
```

Decision model:

| Object | Purpose | Mutability |
| --- | --- | --- |
| proof_event | GitHub event normalized into internal subject | append-only |
| feature_vector | extracted facts for scoring | append-only per subject/head_sha/rule_version |
| score_decision | minutes, reasons, risk flags | append-only per feature/rule_version |
| credit_ledger | user-visible earned/spent/clawback ledger | append-only |

Credit entry types:

| Type | Delta | Source |
| --- | ---: | --- |
| `provisional_earned` | positive | PR opened/synchronized or commit batch |
| `confirmed_earned` | positive | merged PR |
| `spent` | negative | mobile selected-app use |
| `clawed_back` | negative | revert/duplicate/abuse |
| `override` | positive or policy bypass | emergency/admin |
| `manual_adjustment` | positive/negative | admin/debug only |

Rules:

- Ledger source of truth is server-side after Sprint 4.
- Mobile local credit becomes a cache, not authority.
- Every mobile sync response must include `lastUpdatedAt` and server ledger version.
- Credit cannot be increased by a mobile-only endpoint.
- Score explanation exposes minutes/reasons/risk flags, not raw private repo content.

## 11. Storage And Retention

Default retention:

| Data | Retention |
| --- | --- |
| GitHub installation id/repo id | until revoke/delete |
| normalized repo metadata | until revoke/delete |
| proof event metadata | 1 year or until delete request |
| feature vector | 1 year or until delete request |
| score decision/rationale | 1 year or until delete request |
| credit ledger | user account lifetime, delete/exportable |
| raw webhook payload | short debug retention only, target 7 days max |
| raw diff/patch/file content | not stored by default |
| installation token | not persisted long-term |

User controls before production:

- disconnect GitHub installation
- remove individual repository from app
- delete GitHub-derived metadata
- export ledger and score decisions
- clear mobile cache

Revocation behavior:

```text
installation deleted/suspended
-> mark installation inactive
-> stop enrichment jobs
-> stop new proof credit
-> keep ledger rows unless user requests deletion
-> mobile sync shows disconnected state
```

Deletion behavior:

```text
user delete GitHub data
-> remove installation/repo/proof metadata
-> remove feature vectors and score decisions
-> retain aggregated ledger only if user chooses account history retention
-> otherwise delete ledger rows tied to GitHub source
```

## 12. Mobile Sync Shape

Sprint 4 mobile API must match the existing local concept.

```ts
interface MobileCreditSync {
  remainingMinutes: number;
  blockedTargets: string[];
  strictMode: boolean;
  freeUntil?: string;
  lastUpdatedAt: string;
  ledgerVersion: string;
  source: "server";
}
```

Minimum endpoints:

| API | Method | Purpose |
| --- | --- | --- |
| `/auth/github/start` | GET | begin GitHub App install/connect |
| `/auth/github/callback` | GET | user/session connect if needed |
| `/webhooks/github` | POST | verified GitHub webhook receiver |
| `/credits/today` | GET | mobile credit sync |
| `/activity/feed` | GET | recent proof/decision/ledger rows |
| `/policy` | GET/PUT | blocked targets and schedule sync later |
| `/privacy/github/delete` | POST | delete GitHub-derived data |
| `/privacy/github/export` | GET | export GitHub-derived metadata/ledger |

Do not expose:

- mobile endpoint for arbitrary positive credit mutation
- raw GitHub payload download
- raw diff or file content endpoint

## 13. Implementation Sequence

### PR A: Webhook Security Foundation

- Add `/webhooks/github`
- Preserve raw body
- Verify `X-Hub-Signature-256`
- Require event and delivery headers
- Add inbound event table/storage abstraction
- Add duplicate delivery behavior
- Add tests with official HMAC test vector
- No scoring and no ledger write

### PR B: GitHub App Installation Model

- Add installation/repository tables
- Handle `installation` and `installation_repositories`
- Add repo allowlist state
- Add revoked/suspended state
- No scoring and no ledger write

### PR C: PR Enrichment Job

- Handle `pull_request` events
- Fetch PR files/commits/reviews/checks
- Store normalized metadata and feature vector
- Add `patch_missing` and CI detail flags
- No credit ledger write until idempotency tests pass

### PR D: Score Decision Persistence

- Reuse `packages/scoring`
- Persist score decisions
- Add rule version
- Add risk flags
- Add fixture PRs
- No mobile sync yet

### PR E: Credit Ledger Write

- Add append-only ledger
- Confirmed/provisional credit
- Clawback support
- Idempotency tests
- Admin/debug read-only inspection

### PR F: Mobile Read Sync

- Expose `/credits/today`
- Android can read server state behind feature flag
- Local mock remains fallback
- No mobile positive credit mutation

## 14. Required Tests

P0 before any GitHub runtime merge:

- missing signature returns 401
- invalid signature returns 401
- valid signature with official sample passes
- body mutation fails signature
- duplicate `X-GitHub-Delivery` creates one inbound event and one job max
- unknown event is ignored without throwing
- invalid JSON after signature is stored as parse failure
- no ledger row is written by webhook foundation PR

P0 before ledger write:

- replayed merged PR does not double credit
- `synchronize` same `head_sha` does not create duplicate score
- `synchronize` new `head_sha` creates a new feature vector
- self-review/bot-review excluded from approval credit
- failed CI produces risk flag and no CI bonus
- private repo enrichment never stores raw patch text
- revoke installation stops new enrichment

P1:

- rate limit/backoff handling
- large PR pagination
- PR file count cap
- deleted repo/repo removed from installation
- mobile sync stale ledger version handling

## 15. Go / No-Go Checklist

Go only if:

- Android Gate A smoke has at least one real-device pass record.
- Gate D local privacy copy is acceptable during dogfood.
- This spec is linked from README/source-of-truth docs.
- GitHub App permissions are read-only and minimal.
- HMAC/dedupe tests are part of the first GitHub runtime PR.
- raw diff storage is off by default.
- credit ledger is append-only and idempotent.
- mobile sync is read-only for earned credit.

No-go if:

- proof source data is too sparse and PR-only is still the only planned source.
- users would need to grant broad `Contents` or `Issues` access without clear value.
- raw diff is needed for scoring quality in Sprint 4.
- mobile can mint server credit.
- revoke/delete/export is still undefined.

## 16. Open Decisions

| Decision | Default | When to revisit |
| --- | --- | --- |
| `Contents: read` permission | off | only if PR files metadata cannot classify enough features |
| `push` event | off | if 14-day dogfood shows PR-only proof is too sparse |
| WakaTime/IDE fallback | defer | if Gate C fails for GitHub PR-only |
| raw webhook payload retention | 7 days max target | production privacy/legal review |
| LLM explanation | off for private repos | after metadata-only scoring earns trust |
