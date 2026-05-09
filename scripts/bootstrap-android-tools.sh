#!/usr/bin/env sh
set -eu

ROOT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
JDK_VERSION="21.0.11+10"
JDK_DIR="${VOCABUILDARY_JDK:-$ROOT_DIR/.gradle/jdks/jdk-$JDK_VERSION}"
ANDROID_SDK_DIR="${VOCABUILDARY_ANDROID_SDK:-$ROOT_DIR/.gradle/android-sdk}"
CMDLINE_TOOLS_DIR="$ANDROID_SDK_DIR/cmdline-tools/latest"
JDK_URL="https://api.adoptium.net/v3/binary/latest/21/ga/linux/x64/jdk/hotspot/normal/eclipse?project=jdk"
TOOLS_URL="https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip"

download() {
    url="$1"
    output="$2"
    if command -v curl >/dev/null 2>&1; then
        curl -L "$url" -o "$output"
    elif command -v wget >/dev/null 2>&1; then
        wget -O "$output" "$url"
    else
        echo "curl or wget is required to download Android build tools." >&2
        exit 1
    fi
}

if [ ! -x "$JDK_DIR/bin/javac" ]; then
    echo "Installing local JDK 21 into $JDK_DIR"
    tmp_dir=$(mktemp -d)
    mkdir -p "$(dirname "$JDK_DIR")"
    download "$JDK_URL" "$tmp_dir/jdk.tar.gz"
    tar -xzf "$tmp_dir/jdk.tar.gz" -C "$tmp_dir"
    extracted_dir=$(find "$tmp_dir" -mindepth 1 -maxdepth 1 -type d | head -n 1)
    rm -rf "$JDK_DIR"
    mv "$extracted_dir" "$JDK_DIR"
    rm -rf "$tmp_dir"
fi

export JAVA_HOME="$JDK_DIR"

if [ ! -x "$CMDLINE_TOOLS_DIR/bin/sdkmanager" ]; then
    echo "Installing Android command-line tools into $CMDLINE_TOOLS_DIR"
    tmp_dir=$(mktemp -d)
    mkdir -p "$ANDROID_SDK_DIR/cmdline-tools"
    download "$TOOLS_URL" "$tmp_dir/cmdline-tools.zip"
    unzip -q "$tmp_dir/cmdline-tools.zip" -d "$tmp_dir"
    rm -rf "$CMDLINE_TOOLS_DIR"
    mv "$tmp_dir/cmdline-tools" "$CMDLINE_TOOLS_DIR"
    rm -rf "$tmp_dir"
fi

yes | "$CMDLINE_TOOLS_DIR/bin/sdkmanager" --sdk_root="$ANDROID_SDK_DIR" --licenses >/dev/null || true
"$CMDLINE_TOOLS_DIR/bin/sdkmanager" \
    --sdk_root="$ANDROID_SDK_DIR" \
    "platform-tools" \
    "platforms;android-35" \
    "build-tools;35.0.0"

echo "Android build tools are ready."
echo "JAVA_HOME=$JDK_DIR"
echo "ANDROID_HOME=$ANDROID_SDK_DIR"
