#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
cd "$ROOT_DIR"

# shellcheck disable=SC1091
source "$ROOT_DIR/packaging/common/package-config.env"

if [ "$(uname -s)" != "Linux" ]; then
  echo "Linux packages must be built on Linux. Current OS: $(uname -s)" >&2
  exit 1
fi

if ! command -v jpackage >/dev/null 2>&1; then
  echo "jpackage was not found. Install a full Java 21 JDK and ensure jpackage is on PATH." >&2
  exit 1
fi

RAW_ARCH="$(uname -m)"
case "$RAW_ARCH" in
  x86_64|amd64) RELEASE_ARCH="x64" ;;
  aarch64|arm64) RELEASE_ARCH="arm64" ;;
  *) RELEASE_ARCH="$RAW_ARCH" ;;
esac

APP_VERSION="${APP_VERSION_OVERRIDE:-$APP_VERSION}"
DIST_ROOT="$ROOT_DIR/$DIST_DIR/linux"
APP_IMAGE_DIR="$DIST_ROOT/app-image"
PACKAGE_DIR="$DIST_ROOT/packages"
ICON_PATH="$ROOT_DIR/packaging/icons/app.png"
TAR_PATH="$DIST_ROOT/${OUTPUT_ARTIFACT_PREFIX}-${APP_VERSION}-linux-${RELEASE_ARCH}.tar.gz"

echo "Packaging $APP_NAME $APP_VERSION for Linux ($RELEASE_ARCH)"
echo "Java:"
java -version

rm -rf "$DIST_ROOT"
mkdir -p "$APP_IMAGE_DIR" "$PACKAGE_DIR"

"$ROOT_DIR/packaging/common/prepare-input.sh"

# shellcheck disable=SC1091
source "$ROOT_DIR/target/package-metadata.env"

JPACKAGE_COMMON=(
  --name "$APP_NAME"
  --app-version "$PACKAGE_VERSION"
  --vendor "$VENDOR"
  --input "$PACKAGE_INPUT_DIR"
  --main-jar "$PACKAGE_MAIN_JAR"
  --main-class "$MAIN_CLASS"
  --java-options "-Dfile.encoding=UTF-8"
)

JPACKAGE_INSTALLER_OPTIONS=(
  --linux-package-name "academic-tracker"
  --linux-shortcut
)

if [ -f "$ICON_PATH" ]; then
  echo "Using Linux icon: $ICON_PATH"
  JPACKAGE_COMMON+=(--icon "$ICON_PATH")
else
  echo "No Linux icon found at packaging/icons/app.png; packaging without a custom icon."
fi

echo "Creating Linux app image..."
jpackage \
  --type app-image \
  "${JPACKAGE_COMMON[@]}" \
  --dest "$APP_IMAGE_DIR"

APP_IMAGE_PATH="$APP_IMAGE_DIR/$APP_NAME"
if [ ! -d "$APP_IMAGE_PATH" ]; then
  echo "jpackage did not create expected app image: $APP_IMAGE_PATH" >&2
  exit 1
fi

echo "Creating tar.gz fallback artifact..."
tar -C "$APP_IMAGE_DIR" -czf "$TAR_PATH" "$APP_NAME"

created_native_package=false

if command -v dpkg-deb >/dev/null 2>&1; then
  echo "Attempting DEB package..."
  if jpackage --type deb "${JPACKAGE_COMMON[@]}" "${JPACKAGE_INSTALLER_OPTIONS[@]}" --dest "$PACKAGE_DIR"; then
    CREATED_DEB="$(find "$PACKAGE_DIR" -maxdepth 1 -name "*.deb" -type f | sort | head -n 1)"
    if [ -n "$CREATED_DEB" ] && [ -s "$CREATED_DEB" ]; then
      DEB_PATH="$DIST_ROOT/${OUTPUT_ARTIFACT_PREFIX}-${APP_VERSION}-linux-${RELEASE_ARCH}.deb"
      mv "$CREATED_DEB" "$DEB_PATH"
      created_native_package=true
      echo "DEB: $DEB_PATH"
    fi
  else
    echo "DEB packaging failed; keeping app-image/tar.gz fallback." >&2
  fi
else
  echo "dpkg-deb not found; skipping DEB package."
fi

if command -v rpmbuild >/dev/null 2>&1; then
  echo "Attempting RPM package..."
  if jpackage --type rpm "${JPACKAGE_COMMON[@]}" "${JPACKAGE_INSTALLER_OPTIONS[@]}" --dest "$PACKAGE_DIR"; then
    CREATED_RPM="$(find "$PACKAGE_DIR" -maxdepth 1 -name "*.rpm" -type f | sort | head -n 1)"
    if [ -n "$CREATED_RPM" ] && [ -s "$CREATED_RPM" ]; then
      RPM_PATH="$DIST_ROOT/${OUTPUT_ARTIFACT_PREFIX}-${APP_VERSION}-linux-${RELEASE_ARCH}.rpm"
      mv "$CREATED_RPM" "$RPM_PATH"
      created_native_package=true
      echo "RPM: $RPM_PATH"
    fi
  else
    echo "RPM packaging failed; keeping app-image/tar.gz fallback." >&2
  fi
else
  echo "rpmbuild not found; skipping RPM package."
fi

find "$DIST_ROOT" \( -name ".DS_Store" -o -name "__MACOSX" \) -print -exec rm -rf {} +

echo
echo "Linux packaging complete."
echo "App image: $APP_IMAGE_PATH"
echo "Tarball:   $TAR_PATH"
if [ "$created_native_package" = false ]; then
  echo "No DEB/RPM installer was created; use the tar.gz fallback or install required packaging tools."
fi
