#!/usr/bin/env bash
#
# Idempotent Cloud Agent bootstrap for the Diplomat Android app.
# Installs the Android command-line tools + SDK packages required to build,
# points Gradle at them, and primes the Gradle caches with a debug build.
set -euo pipefail

ANDROID_SDK_ROOT="${ANDROID_SDK_ROOT:-$HOME/android-sdk}"
CMDLINE_TOOLS_ZIP="commandlinetools-linux-15859902_latest.zip"
PLATFORM="platforms;android-36"
BUILD_TOOLS="build-tools;36.0.0"

mkdir -p "$ANDROID_SDK_ROOT"

# 1. Command-line tools (only download when missing).
if [ ! -x "$ANDROID_SDK_ROOT/cmdline-tools/latest/bin/sdkmanager" ]; then
    echo "Installing Android command-line tools..."
    tmp_dir="$(mktemp -d)"
    curl -sSL -o "$tmp_dir/cmdtools.zip" \
        "https://dl.google.com/android/repository/$CMDLINE_TOOLS_ZIP"
    mkdir -p "$ANDROID_SDK_ROOT/cmdline-tools"
    unzip -q -o "$tmp_dir/cmdtools.zip" -d "$ANDROID_SDK_ROOT/cmdline-tools"
    rm -rf "$ANDROID_SDK_ROOT/cmdline-tools/latest"
    mv "$ANDROID_SDK_ROOT/cmdline-tools/cmdline-tools" \
        "$ANDROID_SDK_ROOT/cmdline-tools/latest"
    rm -rf "$tmp_dir"
fi

SDKMANAGER="$ANDROID_SDK_ROOT/cmdline-tools/latest/bin/sdkmanager"

# 2. Accept licenses and install the SDK packages (both are idempotent).
yes | "$SDKMANAGER" --sdk_root="$ANDROID_SDK_ROOT" --licenses >/dev/null
"$SDKMANAGER" --sdk_root="$ANDROID_SDK_ROOT" \
    "platform-tools" "$PLATFORM" "$BUILD_TOOLS"

# 3. Point Gradle at the SDK (local.properties is intentionally not committed).
echo "sdk.dir=$ANDROID_SDK_ROOT" > local.properties

# 4. Prime the Gradle wrapper distribution + dependency caches and compile.
./gradlew --no-daemon :app:assembleDebug
