# Commit-to-Unlock

Commit-to-Unlock is a developer self-regulation product: verified developer proof-of-work becomes leisure credit for selected distracting apps.

The project is intentionally build-first. There are no interviews, surveys, landing-page validation, payments, parent/school controls, or GitHub scoring in the current sprint. The first product risk is whether local mobile enforcement is useful enough to keep enabled.

## Current Status

Phase 1 is closed as `Android local blocker code-complete, evidence-gated`.

That means the runnable prototype, policy logic, dogfood logging, analyzer, design guardrails, and CI baseline are complete enough for local dogfood. It does not mean the product is validated. Real-device smoke evidence and a 14-day dogfood data set are still required before GitHub scoring, sync, or monetization work resumes.

The current runnable prototype is Android-only and local-only.

It can:

- ask a playful developer-only entry question on first launch
- store local mock credit state
- evaluate local policy reason codes for weekdays, time windows, manual holiday, mock free day, emergency unlock, and credit
- add daily quest plans and complete them with local mock proof before free day is granted
- detect the foreground app through `UsageStatsManager`
- block selected package names with an overlay when credit is `0`
- normalize/reject unsafe target packages before saving, including this app, empty/duplicate entries, launcher/settings/permission-controller, and core system services
- spend `1` mock credit minute after `60` seconds of foreground use on blocked targets
- show a dogfood summary for the last 14 days
- show an in-app dogfood review with Data Quality and Gate A/B/C/D status
- show a recent dogfood event log in the app
- export structured dogfood events as TSV through Android share sheet or `adb`
- distinguish desired monitor state from heartbeat-backed runtime state

The Android emulator smoke flow has been verified on Android 13: selected Chrome target blocks at `0` mock minutes, unlocks after adding `5` test minutes, and spends `1` minute after `60` seconds of foreground use. A physical-device smoke pass is still required before treating MVP-A as evidence-complete.

It does not yet:

- connect to GitHub
- score PRs or commits
- sync with a server
- use AccessibilityService
- lock the whole phone
- prevent uninstall
- handle payments or money stakes

## Product Direction

The product should not compete as a generic screen-time blocker. The recommended position is:

> Verified dev work becomes guilt-free leisure credit.

Core principles:

- Fun developer tone, serious policy boundaries.
- Explain before restrict.
- Ledger over score.
- Proof over self-report.
- Local-first enforcement before GitHub scoring.
- Privacy by default.
- No shame copy.
- Policy-compliant control.

Read these docs in order before changing product direction or implementation priority:

- [MVP execution plan](docs/mvp-execution-plan.md)
- [Product and security hardening plan](docs/product-security-hardening-plan.md)
- [Competitive service review](docs/competitive-service-review.md)
- [MVP gap analysis](docs/mvp-gap-analysis.md)
- [Android dogfood runbook](docs/android-dogfood-runbook.md)
- [Decision log](docs/decision-log.md)
- [Security and logic review](docs/security-and-logic-review.md)
- [GitHub Sprint 4 entry spec](docs/github-sprint4-entry.md)
- [Control and account design](docs/control-account-design.md)
- [App design](docs/app-design.md)
- [Proof policy MVP](docs/proof-policy-mvp.md)

Old snapshot/planning docs were removed after their still-valid decisions were folded into the active docs above. Use git history for the deleted references when historical context is needed.

## Repository Layout

```text
apps/
  android/  Android local blocker prototype
  api/      Fastify health-only scaffold; GitHub scoring is parked until Sprint 4
  ios/      SwiftUI and Screen Time API source/design skeleton
docs/       Current product, market, technical, and execution planning
fixtures/   Cross-platform policy golden fixtures
packages/
  scoring/  Rules-first scoring package scaffold, not wired to the API yet
  shared/   Shared TypeScript contracts
scripts/    Local dogfood helper scripts
```

## Prerequisites

- Node.js 22+
- pnpm 10+
- JDK 17
- Android SDK 33
- Android platform-tools for `adb`

The Android Gradle Wrapper is committed, so global Gradle is not required.

## Common Commands

```bash
pnpm test
pnpm build
pnpm typecheck
./gradlew :apps:android:testDebugUnitTest
./gradlew :apps:android:assembleDebug
./gradlew :apps:android:lintDebug
```

Android dogfood helpers:

```bash
pnpm android:dogfood
pnpm android:dogfood:export
pnpm android:dogfood:analyze
```

Use `ANDROID_SERIAL=<device-id>` when more than one Android device is connected.

## Android Dogfood Flow

For repeated device testing and gate decisions, follow the detailed [Android dogfood runbook](docs/android-dogfood-runbook.md).

1. Connect an Android device with USB debugging enabled.
2. Install and launch:

   ```bash
   pnpm android:dogfood
   ```

3. Grant Usage Access, Display over other apps, and Notifications.
4. Add a target package, for example `com.android.chrome`.
5. Save the policy schedule. Default weekdays are Monday-Friday; leave active time blank for all day.
6. Reset credit to `0`, start the monitor, and open the target app.
7. Confirm the overlay shows a policy reason such as `credit_empty`.
8. Add test credit and keep the target app foreground for 60 seconds.
9. Add a required daily quest, then complete it with mock proof and confirm `free_day`.
10. Test one exception path: mock free day, manual holiday, or emergency unlock.
11. Export the dogfood TSV:

   ```bash
   pnpm android:dogfood:export
   ```

12. Analyze the newest export:

   ```bash
   pnpm android:dogfood:analyze
   ```

Exports are written under `artifacts/android-dogfood/`, which is intentionally ignored by git.

## Commit And PR Workflow

Work from `dev`, not `main`.

Use task-scoped branches:

```text
feature/<task-name>
fix/<task-name>
docs/<task-name>
chore/<task-name>
refactor/<task-name>
test/<task-name>
```

Keep commits split by task. Do not bundle unrelated implementation, docs, and tooling changes into one commit when they can be reviewed independently.

For PRs with multiple task commits, preserve the commits with a normal merge instead of squash merging unless the user explicitly asks for squash.

Before merging, run:

```bash
pnpm test
pnpm build
pnpm typecheck
./gradlew :apps:android:assembleDebug :apps:android:lintDebug
```

## After Phase 1

Current recommended sequence is maintained in [MVP execution plan](docs/mvp-execution-plan.md#10-remaining-work-plan). Short version:

1. Run and document physical-device Android dogfood smoke evidence.
2. Draft the browser/desktop companion spike before treating mobile-only as a paid product.
3. Start GitHub Sprint 4 with webhook HMAC/dedupe, not scoring.
4. Build credit ledger foundation before mobile API sync.

Do not build payments, school/parent mode, leaderboard, money stakes, or full-diff AI scoring yet.
