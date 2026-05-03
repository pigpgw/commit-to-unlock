#!/usr/bin/env sh
set -eu

GRADLE_VERSION="8.7"
BASE_DIR="$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)"
GRADLE_USER_HOME="${GRADLE_USER_HOME:-"$BASE_DIR/.gradle"}"
if [ -z "${ANDROID_HOME:-}" ] && [ -d "$HOME/Library/Android/sdk" ]; then
  export ANDROID_HOME="$HOME/Library/Android/sdk"
fi
DIST_DIR="$GRADLE_USER_HOME/wrapper/dists/gradle-$GRADLE_VERSION-bin"
GRADLE_BIN="$DIST_DIR/gradle-$GRADLE_VERSION/bin/gradle"
ZIP_PATH="$DIST_DIR/gradle-$GRADLE_VERSION-bin.zip"
DIST_URL="https://services.gradle.org/distributions/gradle-$GRADLE_VERSION-bin.zip"

if [ ! -x "$GRADLE_BIN" ]; then
  mkdir -p "$DIST_DIR"
  if [ ! -f "$ZIP_PATH" ]; then
    echo "Downloading Gradle $GRADLE_VERSION..."
    if command -v curl >/dev/null 2>&1; then
      curl -L "$DIST_URL" -o "$ZIP_PATH"
    elif command -v wget >/dev/null 2>&1; then
      wget "$DIST_URL" -O "$ZIP_PATH"
    else
      echo "curl or wget is required to download Gradle." >&2
      exit 1
    fi
  fi
  unzip -q -o "$ZIP_PATH" -d "$DIST_DIR"
fi

exec "$GRADLE_BIN" "$@"
