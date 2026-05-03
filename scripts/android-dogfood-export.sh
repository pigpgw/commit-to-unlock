#!/usr/bin/env bash
set -euo pipefail

usage() {
  cat <<'USAGE'
Usage: scripts/android-dogfood-export.sh [output-file]

Pulls the latest Commit Unlock dogfood TSV export from a connected debug device.
If output-file is omitted, the file is written under artifacts/android-dogfood/.

Set ANDROID_SERIAL when more than one device is connected.
USAGE
}

if [[ "${1:-}" == "--" ]]; then
  shift
fi

if [[ "${1:-}" == "-h" || "${1:-}" == "--help" ]]; then
  usage
  exit 0
fi

script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
repo_root="$(cd "$script_dir/.." && pwd)"
package_name="com.commitunlock.prototype"
remote_file="files/dogfood-export.tsv"

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

if [[ $# -gt 1 ]]; then
  echo "Too many arguments." >&2
  usage >&2
  exit 2
fi

if [[ $# -eq 1 ]]; then
  output_path="$1"
else
  timestamp="$(date -u +%Y%m%dT%H%M%SZ)"
  output_path="$repo_root/artifacts/android-dogfood/dogfood-export-$timestamp.tsv"
fi

mkdir -p "$(dirname "$output_path")"
temp_path="$output_path.tmp"

if ! "${adb_cmd[@]}" exec-out run-as "$package_name" cat "$remote_file" > "$temp_path"; then
  rm -f "$temp_path"
  cat >&2 <<'ERROR'
Could not read dogfood export from the device.

Make sure:
1. A debug build is installed with `pnpm android:dogfood`.
2. The app has recorded at least one dogfood event.
3. The package is debuggable so `run-as com.commitunlock.prototype` works.
ERROR
  exit 1
fi

if [[ ! -s "$temp_path" ]]; then
  rm -f "$temp_path"
  echo "Dogfood export is empty. Open the app and record at least one event first." >&2
  exit 1
fi

mv "$temp_path" "$output_path"
echo "Wrote dogfood export to $output_path"
