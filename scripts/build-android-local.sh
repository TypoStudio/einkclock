#!/usr/bin/env bash
# 로컬 Android APK 빌드 스크립트
# 사용법: bash scripts/build-android-local.sh [debug|release]
set -e

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
ROOT_DIR="$(dirname "$SCRIPT_DIR")"
ANDROID_DIR="$ROOT_DIR/android"
ASSETS_DIR="$ANDROID_DIR/app/src/main/assets"
BUILD_TYPE="${1:-debug}"

echo "▶ index.html → assets 복사"
cp "$ROOT_DIR/index.html" "$ASSETS_DIR/index.html"

echo "▶ Gradle $BUILD_TYPE 빌드"
cd "$ANDROID_DIR"

if [ -f "gradlew" ] && [ -f "gradle/wrapper/gradle-wrapper.jar" ]; then
    ./gradlew "assemble${BUILD_TYPE^}"
elif command -v gradle &>/dev/null; then
    gradle "assemble${BUILD_TYPE^}"
else
    echo "오류: Gradle이 설치되어 있지 않습니다."
    echo "  - Android Studio로 android/ 폴더를 열거나"
    echo "  - brew install gradle (macOS) 로 설치하세요."
    exit 1
fi

APK_PATH=$(find "$ANDROID_DIR/app/build/outputs/apk/$BUILD_TYPE" -name "*.apk" | head -1)
echo ""
echo "✓ 빌드 완료: $APK_PATH"
