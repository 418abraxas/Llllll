#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"

export ANDROID_HOME="${ANDROID_HOME:-$HOME/android-sdk}"
SDKM="$ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager"

command -v unzip >/dev/null || { sudo apt-get update -y && sudo apt-get install -y unzip; }

if [ ! -x "$SDKM" ]; then
  mkdir -p "$ANDROID_HOME/cmdline-tools"
  curl -fsSL -o /tmp/cmdtools.zip https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip
  unzip -q -o /tmp/cmdtools.zip -d "$ANDROID_HOME/cmdline-tools"
  rm -rf "$ANDROID_HOME/cmdline-tools/latest"
  mv "$ANDROID_HOME/cmdline-tools/cmdline-tools" "$ANDROID_HOME/cmdline-tools/latest"
fi

yes | "$SDKM" --licenses >/dev/null || true
"$SDKM" "platform-tools" "platforms;android-34" "build-tools;34.0.0"
echo "sdk.dir=$ANDROID_HOME" > local.properties

if [ ! -f gradle/wrapper/gradle-wrapper.jar ]; then
  curl -fsSL -o /tmp/gradle.zip https://services.gradle.org/distributions/gradle-8.7-bin.zip
  unzip -q -o /tmp/gradle.zip -d "$HOME/gradle-dist"
  "$HOME/gradle-dist/gradle-8.7/bin/gradle" wrapper --gradle-version 8.7
fi
chmod +x gradlew
echo "Setup done. Run: ./gradlew testDebugUnitTest assembleDebug"
