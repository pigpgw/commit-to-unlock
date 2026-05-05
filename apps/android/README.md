# Android Local Blocker

This is the first runnable Commit-to-Unlock product surface. It is a local-only Android prototype that blocks selected package names with mock leisure credit. It does not connect to GitHub, the API, payments, or any account system.

## Status

`RC 0.1` is ready for emulator demo and private dogfood. The next evidence gate is physical-device smoke plus a 14-day dogfood run.

Verified on `CommitUnlockApi33` Android 13 emulator on 2026-05-06:

- first-run developer gate and rejection branch render correctly
- permission status appears on the home dashboard
- setup checklist explains missing prerequisites before monitor start
- notification permission is user-triggered from the permission section, not an automatic first-run prompt
- `com.android.chrome` can be saved as a selected target
- local mock credit is capped at `240` minutes
- reliable Chrome demo reset sets all days, zero credit, Chrome target, and no bypasses
- selected target + `0` mock credit displays the blocking overlay
- selected target + positive mock credit allows access
- foreground target use spends `1` mock minute after `60` seconds
- policy, daily quest, emergency unlock, and dogfood review sections render without layout overlap

Screenshots are in [../../docs/assets/screenshots/android](../../docs/assets/screenshots/android).

## Product Boundaries

The Android app intentionally uses:

- `UsageStatsManager` for foreground-app detection
- Android overlay permission for the blocking screen
- a foreground service for local monitoring
- local-only preferences and dogfood event logs

It intentionally does not use:

- AccessibilityService
- Device Admin
- uninstall prevention
- whole-phone lock
- installed-app scanning
- server sync

The user chooses target packages manually or from recent foreground packages. The app rejects unsafe targets such as this app, launcher, settings, permission controller, and core system services.

## Local State

The shared local credit contract is:

```kotlin
data class CreditState(
    val remainingMinutes: Int,
    val blockedTargets: List<String>,
    val strictMode: Boolean,
    val lastUpdatedAt: String
)
```

Android also stores local policy fields:

- active weekdays
- optional active time window
- manual holiday today
- emergency unlock
- free day from mock proof
- daily quest rows
- dogfood event log

## Quick Run

Requirements:

- JDK 17
- Android SDK 33
- Android platform-tools
- one Android device or emulator

Install and launch the debug app:

```bash
pnpm android:dogfood
```

When multiple devices are connected:

```bash
ANDROID_SERIAL=<device-id> pnpm android:dogfood
```

## Required Permissions

Grant these from the app flow:

- Usage Access
- Display over other apps
- Notifications on Android 13+

The app should report:

- `Usage Access: granted`
- `Overlay Permission: granted`
- `Monitor desired: disabled` before the monitor starts
- `Monitor service: stopped` before the monitor starts

## Smoke Test

Use this exact path for every release-candidate check.

1. Launch the app.
2. On first launch, answer `예, 커밋으로 증명하겠습니다`.
3. Grant Usage Access, overlay, and notification permissions.
4. Open Chrome once, return to Commit Unlock, and confirm `com.android.chrome` appears in recent external packages.
5. Add `com.android.chrome` as a blocked target.
   - For the fastest repeatable setup, tap `Prepare reliable Chrome demo`.
6. Save the policy schedule. Leave time fields empty for all-day behavior.
7. Tap `Reset credit to 0`.
8. Tap `Start monitor service`.
9. Open Chrome.
10. Confirm the blocking overlay appears with reason `credit_empty`.
11. Return to Commit Unlock and tap `Add 5 test minutes`.
12. Open Chrome again.
13. Confirm the overlay does not stay visible while credit is above `0`.
14. Keep Chrome foreground for at least `60` seconds.
15. Confirm one mock minute is spent automatically.
16. Add a required daily quest and confirm planned tasks do not unlock anything by themselves.
17. Complete the next quest with mock proof and confirm the policy reason becomes `free_day`.
18. Enable strict mode, reset credit to `0`, and confirm the overlay no longer shows the test-credit shortcut.

## Dogfood Data

The prototype stores the latest 1,000 local dogfood events and can export a TSV with:

- `timestamp`
- `type`
- `target`
- `policy_reason`
- `credit_remaining`
- `detail`

The in-app `Share redacted dogfood export` action keeps policy reasons and credit values but hides target package names, quest titles, and emergency reasons before opening the Android share sheet.

Export and analyze:

```bash
pnpm android:dogfood:export
pnpm android:dogfood:analyze
```

Default export location:

```text
artifacts/android-dogfood/
```

That directory is ignored by git because it is local validation evidence, not source.

## Build And Test

```bash
./gradlew :apps:android:testDebugUnitTest
./gradlew :apps:android:assembleDebug
./gradlew :apps:android:lintDebug
```

## Useful Scripts

| Command | Purpose |
| --- | --- |
| `pnpm android:dogfood` | Build, install, and launch the debug app. |
| `pnpm android:dogfood -- --skip-build` | Install the existing debug APK without rebuilding. |
| `pnpm android:dogfood -- --no-launch` | Install but do not launch the app. |
| `pnpm android:dogfood:export` | Pull the current dogfood TSV into `artifacts/android-dogfood/`. |
| `pnpm android:dogfood:export -- path/to/file.tsv` | Pull dogfood TSV to a chosen path. |
| `pnpm android:dogfood:analyze` | Analyze the newest exported TSV. |
| `pnpm android:dogfood:analyze path/to/file.tsv --json` | Analyze a specific TSV and print JSON. |

## Troubleshooting

### `No connected Android device found`

- Check USB debugging.
- Run `adb devices -l`.
- Reconnect the device and accept the trust prompt.

### `Multiple Android devices found`

Set the target explicitly:

```bash
ANDROID_SERIAL=<device-id> pnpm android:dogfood
ANDROID_SERIAL=<device-id> pnpm android:dogfood:export
```

### Overlay does not appear

- Confirm Usage Access is granted.
- Confirm Display over other apps is granted.
- Confirm the blocked package exactly matches the foreground package shown in the app.
- Reset credit to `0`.
- Confirm the monitor service is running.
- Confirm current weekday/time policy is active and no free-day or emergency exception is active.

### Dogfood export is empty

- Open the app once after installing.
- Save a target or start and stop the monitor to create events.
- Run `pnpm android:dogfood:export` again.
