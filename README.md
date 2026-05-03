# Commit-to-Unlock

Commit-to-Unlock is a developer self-regulation product: verified developer proof-of-work becomes leisure credit for selected distracting apps.

The project is intentionally build-first. There are no interviews, surveys, landing-page validation, payments, parent/school controls, or GitHub scoring in the current sprint. The first product risk is whether local mobile enforcement is useful enough to keep enabled.

## Current Status

The current runnable prototype is Android-only and local-only.

It can:

- store local mock credit state
- detect the foreground app through `UsageStatsManager`
- block selected package names with an overlay when credit is `0`
- spend `1` mock credit minute after `60` seconds of foreground use on blocked targets
- show a dogfood summary for the last 14 days
- export dogfood events as TSV through Android share sheet or `adb`

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

- Explain before restrict.
- Ledger over score.
- Proof over self-report.
- Local-first enforcement before GitHub scoring.
- Privacy by default.
- No shame copy.
- Policy-compliant control.

Read the strategy docs before changing product direction:

- [Product strategy](docs/product-strategy-spec.md)
- [Market needs and pivot plan](docs/market-needs-and-pivot-plan.md)
- [App design](docs/app-design.md)
- [Build-first execution plan](docs/build-first-execution-plan.md)
- [Decision log](docs/decision-log.md)

## Repository Layout

```text
apps/
  android/  Android local blocker prototype
  api/      Fastify API scaffold for later GitHub scoring work
  ios/      SwiftUI and Screen Time API source/design skeleton
docs/       Product, market, technical, and execution planning
packages/
  scoring/  Rules-first scoring package scaffold
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
./gradlew :apps:android:assembleDebug
./gradlew :apps:android:lintDebug
```

Android dogfood helpers:

```bash
pnpm android:dogfood
pnpm android:dogfood:export
```

Use `ANDROID_SERIAL=<device-id>` when more than one Android device is connected.

## Android Dogfood Flow

1. Connect an Android device with USB debugging enabled.
2. Install and launch:

   ```bash
   pnpm android:dogfood
   ```

3. Grant Usage Access, Display over other apps, and Notifications.
4. Add a target package, for example `com.android.chrome`.
5. Reset credit to `0`, start the monitor, and open the target app.
6. Add test credit and keep the target app foreground for 60 seconds.
7. Export the dogfood TSV:

   ```bash
   pnpm android:dogfood:export
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

1. Improve dogfood export analysis with a local TSV summary script.
2. Run the Android prototype on a physical device for repeated dogfood sessions.
3. Decide Gate 1 and Gate 2 from dogfood data before returning to GitHub scoring.
4. Prepare iOS Xcode project and Family Controls entitlement work.

Do not build payments, school/parent mode, leaderboard, money stakes, or full-diff AI scoring yet.
