# Commit-to-Unlock

Commit-to-Unlock is a developer self-regulation product: verified developer proof-of-work becomes leisure credit for selected distracting apps.

The project is intentionally build-first. There are no interviews, surveys, landing-page validation, payments, parent/school controls, or GitHub scoring in the current sprint. The first product risk is whether local mobile enforcement is useful enough to keep enabled.

## Current Status

The current runnable prototype is Android-only and local-only.

It can:

- ask a playful developer-only entry question on first launch
- store local mock credit state
- evaluate local policy reason codes for weekdays, time windows, manual holiday, mock free day, emergency unlock, and credit
- add daily quest plans and complete them with local mock proof before free day is granted
- detect the foreground app through `UsageStatsManager`
- block selected package names with an overlay when credit is `0`
- spend `1` mock credit minute after `60` seconds of foreground use on blocked targets
- show a dogfood summary for the last 14 days
- show a recent dogfood event log in the app
- export structured dogfood events as TSV through Android share sheet or `adb`

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

> Verified developer proof-of-work becomes guilt-free screen time.

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
- [Android dogfood runbook](docs/android-dogfood-runbook.md)
- [Decision log](docs/decision-log.md)
- [Security and logic review](docs/security-and-logic-review.md)
- [App design](docs/app-design.md)
- [Proof policy MVP](docs/proof-policy-mvp.md)

Reference docs:

- [Product strategy](docs/product-strategy-spec.md)
- [Design research and UX direction](docs/design-research-and-ux-direction.md)
- [Market needs and pivot plan](docs/market-needs-and-pivot-plan.md)
- [MVP progress audit](docs/mvp-progress-audit.md)
- [Build-first execution plan](docs/build-first-execution-plan.md)
- [Repository audit and cleanup](docs/repository-audit-and-cleanup.md)

## Repository Layout

```text
apps/
  android/  Android local blocker prototype
  api/      Fastify health-only scaffold; GitHub scoring is parked until Sprint 4
  ios/      SwiftUI and Screen Time API source/design skeleton
docs/       Product, market, technical, and execution planning
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
codex/feature/<task>
codex/fix/<task>
codex/docs/<task>
codex/chore/<task>
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

## Next Recommended Work

Current recommended sequence:

1. Follow the [Android dogfood runbook](docs/android-dogfood-runbook.md) on a physical device.
2. Add tests for `DogfoodEventStore` export, parsing, sanitization, retention, and dedupe behavior.
3. Add shared policy golden fixtures for TypeScript and Android.
4. Add Android privacy and permission disclosure UI.
5. Prepare the GitHub Sprint 4 entry spec only after Gate A/B/D have enough evidence.

Do not build payments, school/parent mode, leaderboard, money stakes, or full-diff AI scoring yet.
