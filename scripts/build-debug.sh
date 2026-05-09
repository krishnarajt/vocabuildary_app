#!/usr/bin/env sh
set -eu

ROOT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
JDK_VERSION="21.0.11+10"

"$ROOT_DIR/scripts/bootstrap-android-tools.sh"

export JAVA_HOME="${VOCABUILDARY_JDK:-$ROOT_DIR/.gradle/jdks/jdk-$JDK_VERSION}"
export ANDROID_HOME="${VOCABUILDARY_ANDROID_SDK:-$ROOT_DIR/.gradle/android-sdk}"
export ANDROID_SDK_ROOT="$ANDROID_HOME"

"$ROOT_DIR/gradlew" assembleDebug "$@"
