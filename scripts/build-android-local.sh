#!/usr/bin/env bash
# 로컬 Android APK 빌드 스크립트
# 사용법: bash scripts/build-android-local.sh [debug|release]
set -e

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
ROOT_DIR="$(dirname "$SCRIPT_DIR")"
ANDROID_DIR="$ROOT_DIR/android"
ASSETS_DIR="$ANDROID_DIR/app/src/main/assets"
BUILD_TYPE="${1:-debug}"

# JAVA_HOME 자동 감지: Android Studio 내장 JDK 우선, 없으면 시스템 Java
if [ -z "$JAVA_HOME" ]; then
    AS_JBR="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
    if [ -d "$AS_JBR" ]; then
        export JAVA_HOME="$AS_JBR"
    fi
fi

# ANDROID_HOME 자동 감지
if [ -z "$ANDROID_HOME" ] && [ -d "$HOME/Library/Android/sdk" ]; then
    export ANDROID_HOME="$HOME/Library/Android/sdk"
fi

echo "▶ Java: $(${JAVA_HOME:+$JAVA_HOME/bin/}java -version 2>&1 | head -1)"
echo "▶ index.html → assets 복사"
cp "$ROOT_DIR/index.html" "$ASSETS_DIR/index.html"
echo "▶ locales/ → assets 복사"
cp -r "$ROOT_DIR/locales" "$ASSETS_DIR/"

echo "▶ Gradle $BUILD_TYPE 빌드"
cd "$ANDROID_DIR"

BUILD_TYPE_CAP="$(echo "${BUILD_TYPE:0:1}" | tr '[:lower:]' '[:upper:]')${BUILD_TYPE:1}"

if [ -f "gradlew" ]; then
    ./gradlew "assemble${BUILD_TYPE_CAP}"
elif command -v gradle &>/dev/null; then
    gradle "assemble${BUILD_TYPE_CAP}"
else
    echo "오류: Gradle이 설치되어 있지 않습니다."
    echo "  - Android Studio로 android/ 폴더를 열거나"
    echo "  - brew install gradle (macOS) 로 설치하세요."
    exit 1
fi

APK_PATH=$(find "$ANDROID_DIR/app/build/outputs/apk/$BUILD_TYPE" -name "*.apk" | head -1)
echo ""
echo "✓ 빌드 완료: $APK_PATH"
