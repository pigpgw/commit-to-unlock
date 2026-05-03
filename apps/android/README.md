# Android Local Blocker Prototype

This is the first runnable Commit-to-Unlock mobile prototype. It does not connect to GitHub or the API yet.

## Behavior

- Shows a first-launch developer gate:
  - `예` stores local acceptance and opens the prototype.
  - `아니오` shows a playful rejection screen and exits.
  - This is product tone, not real identity verification.
- Stores local mock credit state:
  - `remainingMinutes`
  - `blockedTargets`
  - `freeUntil`
  - `strictMode`
  - `lastUpdatedAt`
- Stores local policy state:
  - active weekdays
  - optional active time window
  - manual holiday today
  - public holiday behavior placeholder
  - timezone
- Stores emergency unlocks locally with reason, duration, `startedAt`, and `expiresAt`.
- Uses `UsageStatsManager` to detect the foreground app.
- Uses Android overlay permission to show a blocking screen when a blocked package is foreground and mock credit is `0`.
- Applies policy reasons before credit: inactive weekday, outside active time, manual holiday, mock free day, and emergency unlock all allow access without spending credit.
- Automatically spends `1` local credit minute for every `60` seconds of foreground use on blocked targets while the device is interactive.
- Shows target, remaining credit, strict mode, and next action copy on the blocking overlay.
- Shows the latest detected foreground package and a recent local dogfood event log for device testing.
- Shows recent foreground packages from UsageStats so a target can be added without installed-app scanning.
- Hides the overlay test-credit shortcut when strict mode is enabled.
- Stores a structured dogfood event log for the last 1,000 local events and keeps a local TSV export file for `adb` collection.
- Does not use AccessibilityService.

## Quick Dogfood Loop

Use this path for repeated local testing on a physical device.

1. Connect one Android device with USB debugging enabled.
2. Install and launch:

   ```bash
   pnpm android:dogfood
   ```

3. Grant:
   - Usage Access
   - Display over other apps
   - Notifications, on Android 13+

4. Run the smoke test below.
5. Pull the latest dogfood TSV after testing:

   ```bash
   pnpm android:dogfood:export
   ```

When multiple devices are connected, set `ANDROID_SERIAL=<device-id>` before running dogfood scripts.

## Manual Build

Use this when checking Gradle output without installing on a device.

```bash
./gradlew :apps:android:assembleDebug
./gradlew :apps:android:lintDebug
```

## Dogfood Scripts

| Command | Purpose |
| --- | --- |
| `pnpm android:dogfood` | Build, install, and launch the debug app. |
| `pnpm android:dogfood -- --skip-build` | Install the existing debug APK without rebuilding. |
| `pnpm android:dogfood -- --no-launch` | Install but do not launch the app. |
| `pnpm android:dogfood:export` | Pull the current dogfood TSV into `artifacts/android-dogfood/`. |
| `pnpm android:dogfood:export -- path/to/file.tsv` | Pull the current dogfood TSV to a chosen path. |

## Device Smoke Test

1. Connect a physical Android device:

   ```bash
   ~/Library/Android/sdk/platform-tools/adb devices -l
   ```

2. Install and launch the debug APK:

   ```bash
   pnpm android:dogfood
   ```

3. Open Commit Unlock and grant:
   - Usage Access
   - Display over other apps
   - Notifications on Android 13+

4. On first launch, answer `예, 커밋으로 증명하겠습니다`.
5. Tap `Refresh status` and confirm:
   - `Usage Access: granted`
   - `Overlay Permission: granted`
   - `Monitor service: stopped`

6. Open Chrome once, return to Commit Unlock, and confirm `com.android.chrome` appears under recent external packages.
7. Tap `Add latest external package`, or manually put `com.android.chrome` in blocked packages and tap `Save blocked packages`.
8. Leave weekdays as Monday-Friday, leave active time blank, and tap `Save policy schedule`.
9. Tap `Reset credit to 0`.
10. Tap `Start monitor service`.
11. Open Chrome. The blocking overlay should appear within a few seconds and show target, reason `credit_empty`, remaining credit, strict mode, and next action text.
12. Return to Commit Unlock and tap `Add 5 test minutes`.
13. Open Chrome again. The overlay should not stay visible while mock credit is above `0`.
14. Keep Chrome foreground for at least 60 seconds and confirm one minute is spent automatically.
15. Reset credit to `0`, enter an emergency reason, tap `Emergency unlock 5 minutes`, then open Chrome. The overlay should not appear until the unlock expires.
16. Tap `Set mock free day until midnight` and confirm the policy summary reason becomes `free_day`.
17. Enable strict mode, reset credit to `0`, and open Chrome again. The overlay should not show the `Add 5 test minutes` shortcut.

Use the in-app dogfood event log to inspect permission, foreground, target-match, overlay, and credit events.

## Dogfood Export

The prototype includes a `Dogfood summary (last 14 days)` section for build-first validation.

Use it to track:

- monitor enabled days
- blocked attempts
- permission failures
- overlay open-app actions
- overlay test-credit unlocks
- automatic credit spends
- manual credit changes
- policy blocks
- emergency unlocks
- mock free days

Tap `Share dogfood export` to export a TSV with `timestamp`, `type`, and `detail` columns. This is local-only and does not upload data to any server.

For repeat dogfood runs on a debug build, pull the latest export from the connected device:

```bash
pnpm android:dogfood:export
```

By default this writes to `artifacts/android-dogfood/`. Set `ANDROID_SERIAL` first when multiple devices are connected.

## Troubleshooting

### `No connected Android device found`

- Check USB debugging is enabled.
- Run `adb devices -l`.
- Reconnect the device and accept the trust prompt.

### `Multiple Android devices found`

Set the target explicitly:

```bash
ANDROID_SERIAL=<device-id> pnpm android:dogfood
ANDROID_SERIAL=<device-id> pnpm android:dogfood:export
```

### Dogfood export is empty

- Open the app once after installing.
- Start and stop the monitor, or save targets, to create at least one dogfood event.
- Then run `pnpm android:dogfood:export` again.

### Overlay does not appear

- Confirm Usage Access is granted.
- Confirm Display over other apps is granted.
- Confirm the blocked package exactly matches the foreground package shown in the app.
- Reset credit to `0`.
- Confirm monitor service is running.
