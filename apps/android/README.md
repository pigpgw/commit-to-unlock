# Android Local Blocker Prototype

This is the first runnable Commit-to-Unlock mobile prototype. It does not connect to GitHub or the API yet.

## Behavior

- Stores local mock credit state:
  - `remainingMinutes`
  - `blockedTargets`
  - `strictMode`
  - `lastUpdatedAt`
- Uses `UsageStatsManager` to detect the foreground app.
- Uses Android overlay permission to show a blocking screen when a blocked package is foreground and mock credit is `0`.
- Shows the latest detected foreground package and a bounded local debug log for device testing.
- Shows recent foreground packages from UsageStats so a target can be added without installed-app scanning.
- Hides the overlay test-credit shortcut when strict mode is enabled.
- Does not use AccessibilityService.

## Local Run

1. Install Android SDK 33 and JDK 17.
2. Build:

   ```bash
   ./gradlew :apps:android:assembleDebug
   ```

3. Install:

   ```bash
   ~/Library/Android/sdk/platform-tools/adb install -r apps/android/build/outputs/apk/debug/android-debug.apk
   ```

4. Open the app and grant:
   - Usage Access
   - Display over other apps
   - Notifications, on Android 13+

5. Add package names to block, for example `com.instagram.android`, or open a target app once and use `Add latest external package`.
6. Start the monitor service.

## Device Smoke Test

1. Connect a physical Android device:

   ```bash
   ~/Library/Android/sdk/platform-tools/adb devices -l
   ```

2. Install the debug APK:

   ```bash
   ~/Library/Android/sdk/platform-tools/adb install -r apps/android/build/outputs/apk/debug/android-debug.apk
   ```

3. Open Commit Unlock and grant:
   - Usage Access
   - Display over other apps
   - Notifications on Android 13+

4. Tap `Refresh status` and confirm:
   - `Usage Access: granted`
   - `Overlay Permission: granted`
   - `Monitor service: stopped`

5. Open Chrome once, return to Commit Unlock, and confirm `com.android.chrome` appears under recent external packages.
6. Tap `Add latest external package`, or manually put `com.android.chrome` in blocked packages and tap `Save blocked packages`.
7. Tap `Reset credit to 0`.
8. Tap `Start monitor service`.
9. Open Chrome. The blocking overlay should appear within a few seconds.
10. Return to Commit Unlock and tap `Add 5 test minutes`.
11. Open Chrome again. The overlay should not stay visible while mock credit is above `0`.
12. Enable strict mode, reset credit to `0`, and open Chrome again. The overlay should not show the `Add 5 test minutes` shortcut.

Use the in-app debug log to inspect permission, foreground, target-match, overlay, and credit events.
