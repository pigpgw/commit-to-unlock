#!/usr/bin/env bash
set -euo pipefail

usage() {
  cat <<'USAGE'
Usage: scripts/android-dogfood-install.sh [--skip-build] [--no-launch]

Builds, installs, and launches the Android local blocker prototype on one
connected Android device.

Options:
  --skip-build   Install the existing debug APK without running Gradle first.
  --no-launch    Install the APK but do not launch the app.
  -h, --help     Show this help text.

Set ANDROID_SERIAL when more than one device is connected.
USAGE
}

skip_build=0
launch_app=1

while [[ $# -gt 0 ]]; do
  case "$1" in
    --skip-build)
      skip_build=1
      ;;
    --no-launch)
      launch_app=0
      ;;
    -h|--help)
      usage
      exit 0
      ;;
    *)
      echo "Unknown option: $1" >&2
      usage >&2
      exit 2
      ;;
  esac
  shift
done

script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
repo_root="$(cd "$script_dir/.." && pwd)"
apk_path="$repo_root/apps/android/build/outputs/apk/debug/android-debug.apk"
app_component="com.commitunlock.prototype/.MainActivity"

find_adb() {
  if command -v adb >/dev/null 2>&1; then
    command -v adb
    return
  fi

  if [[ -n "${ANDROID_HOME:-}" && -x "$ANDROID_HOME/platform-tools/adb" ]]; then
    printf '%s\n' "$ANDROID_HOME/platform-tools/adb"
    return
  fi

  if [[ -n "${ANDROID_SDK_ROOT:-}" && -x "$ANDROID_SDK_ROOT/platform-tools/adb" ]]; then
    printf '%s\n' "$ANDROID_SDK_ROOT/platform-tools/adb"
    return
  fi

  if [[ -x "$HOME/Library/Android/sdk/platform-tools/adb" ]]; then
    printf '%s\n' "$HOME/Library/Android/sdk/platform-tools/adb"
    return
  fi

  return 1
}

adb_path="$(find_adb || true)"
if [[ -z "$adb_path" ]]; then
  echo "adb not found. Install Android platform-tools or set ANDROID_HOME/ANDROID_SDK_ROOT." >&2
  exit 1
fi

adb_cmd=("$adb_path")
if [[ -n "${ANDROID_SERIAL:-}" ]]; then
  adb_cmd+=("-s" "$ANDROID_SERIAL")
fi

device_count="$("$adb_path" devices | awk 'NR > 1 && $2 == "device" { count++ } END { print count + 0 }')"
if [[ -z "${ANDROID_SERIAL:-}" ]]; then
  if [[ "$device_count" -eq 0 ]]; then
    echo "No connected Android device found. Connect a device and enable USB debugging." >&2
    exit 1
  fi

  if [[ "$device_count" -gt 1 ]]; then
    echo "Multiple Android devices found. Set ANDROID_SERIAL to choose one." >&2
    "$adb_path" devices -l >&2
    exit 1
  fi
fi

cd "$repo_root"

if [[ "$skip_build" -eq 0 ]]; then
  ./gradlew :apps:android:assembleDebug
fi

if [[ ! -f "$apk_path" ]]; then
  echo "Debug APK not found at $apk_path. Run without --skip-build first." >&2
  exit 1
fi

"${adb_cmd[@]}" install -r "$apk_path"

if [[ "$launch_app" -eq 1 ]]; then
  "${adb_cmd[@]}" shell am start -n "$app_component"
fi

cat <<'NEXT_STEPS'

Installed Commit Unlock prototype.

Device dogfood checklist:
1. Grant Usage Access, Display over other apps, and Notifications.
2. Add a target package, such as com.android.chrome.
3. Reset credit to 0, start the monitor, and open the target app.
4. Add 5 test minutes and keep the target app foreground for 60 seconds.
5. Check Dogfood summary and Share dogfood export after testing.
NEXT_STEPS
