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

5. Add package names to block, for example `com.instagram.android`, then start the monitor service.
