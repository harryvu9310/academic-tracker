#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
cd "$ROOT_DIR"

# shellcheck disable=SC1091
source "$ROOT_DIR/packaging/common/package-config.env"

if [ "$(uname -s)" != "Darwin" ]; then
  echo "macOS packages must be built on macOS. Current OS: $(uname -s)" >&2
  echo "Run this script on your Mac: ./packaging/macos/package-macos.sh" >&2
  exit 1
fi

if ! command -v jpackage >/dev/null 2>&1; then
  echo "jpackage was not found. Install a full Java 21 JDK and ensure jpackage is on PATH." >&2
  exit 1
fi

if ! command -v hdiutil >/dev/null 2>&1; then
  echo "hdiutil was not found. macOS DMG fallback packaging requires hdiutil." >&2
  exit 1
fi

RAW_ARCH="$(uname -m)"
case "$RAW_ARCH" in
  arm64) RELEASE_ARCH="arm64" ;;
  x86_64) RELEASE_ARCH="x64" ;;
  *) RELEASE_ARCH="$RAW_ARCH" ;;
esac

APP_VERSION="${APP_VERSION_OVERRIDE:-$APP_VERSION}"
DIST_ROOT="$ROOT_DIR/$DIST_DIR/macos"
APP_IMAGE_DIR="$DIST_ROOT/app-image"
DMG_DIR="$DIST_ROOT/dmg"
ICON_PATH="$ROOT_DIR/packaging/icons/app.icns"
DMG_PATH="$DMG_DIR/${OUTPUT_ARTIFACT_PREFIX}-${APP_VERSION}-macos-${RELEASE_ARCH}.dmg"

echo "Packaging $APP_NAME $APP_VERSION for macOS ($RELEASE_ARCH)"
echo "Java:"
java -version

echo "Cleaning previous macOS package output..."
rm -rf "$DIST_ROOT"
mkdir -p "$APP_IMAGE_DIR" "$DMG_DIR"

"$ROOT_DIR/packaging/common/prepare-input.sh"

# shellcheck disable=SC1091
source "$ROOT_DIR/target/package-metadata.env"

if command -v xattr >/dev/null 2>&1; then
  echo "Clearing macOS extended attributes from jpackage input..."
  xattr -cr "$PACKAGE_INPUT_DIR" || true
  find "$PACKAGE_INPUT_DIR" -exec xattr -c {} + 2>/dev/null || true
fi

JPACKAGE_APP_COMMON=(
  --name "$APP_NAME"
  --app-version "$PACKAGE_VERSION"
  --vendor "$VENDOR"
  --input "$PACKAGE_INPUT_DIR"
  --main-jar "$PACKAGE_MAIN_JAR"
  --main-class "$MAIN_CLASS"
  --java-options "-Dfile.encoding=UTF-8"
  --mac-package-identifier "$APP_ID"
)

if [ -f "$ICON_PATH" ]; then
  echo "Using macOS icon: $ICON_PATH"
  JPACKAGE_APP_COMMON+=(--icon "$ICON_PATH")
else
  echo "No macOS icon found at packaging/icons/app.icns; packaging without a custom icon."
fi

echo "Creating macOS .app image..."
jpackage \
  --type app-image \
  "${JPACKAGE_APP_COMMON[@]}" \
  --dest "$APP_IMAGE_DIR"

APP_PATH="$APP_IMAGE_DIR/$APP_NAME.app"
if [ ! -d "$APP_PATH" ]; then
  echo "jpackage did not create expected app image: $APP_PATH" >&2
  exit 1
fi

"$ROOT_DIR/packaging/macos/fix-macos-xattrs.sh" "$APP_PATH"

JPACKAGE_DMG_COMMON=(
  --type dmg
  --name "$APP_NAME"
  --app-version "$PACKAGE_VERSION"
  --vendor "$VENDOR"
  --app-image "$APP_PATH"
  --dest "$DMG_DIR"
  --mac-package-identifier "$APP_ID"
)

echo "Creating macOS DMG..."
if jpackage "${JPACKAGE_DMG_COMMON[@]}"; then
  CREATED_DMG="$(find "$DMG_DIR" -maxdepth 1 -name "*.dmg" -type f | sort | head -n 1)"
  if [ -z "$CREATED_DMG" ] || [ ! -s "$CREATED_DMG" ]; then
    echo "jpackage reported success but no non-empty DMG was found in $DMG_DIR" >&2
    exit 1
  fi
  if [ "$CREATED_DMG" != "$DMG_PATH" ]; then
    mv "$CREATED_DMG" "$DMG_PATH"
  fi
else
  echo "jpackage DMG creation failed. Falling back to hdiutil using the verified .app image." >&2
  rm -f "$DMG_PATH"
  hdiutil create \
    -volname "$APP_NAME" \
    -srcfolder "$APP_PATH" \
    -ov \
    -format UDZO \
    "$DMG_PATH"
fi

if [ ! -d "$APP_PATH" ]; then
  echo "Missing app image after packaging: $APP_PATH" >&2
  exit 1
fi

if [ ! -s "$DMG_PATH" ]; then
  echo "Missing or empty DMG after packaging: $DMG_PATH" >&2
  exit 1
fi

find "$DIST_ROOT" \( -name ".DS_Store" -o -name "__MACOSX" \) -print -exec rm -rf {} +

echo
echo "macOS packaging complete."
echo "App image: $APP_PATH"
echo "DMG:       $DMG_PATH"
