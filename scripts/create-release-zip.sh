#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

if ! command -v zip >/dev/null 2>&1; then
  echo "zip was not found. Install zip or create the archive with your OS file manager." >&2
  exit 1
fi

VERSION="${1:-1.0.0}"
ARTIFACT_DIR="$ROOT_DIR/releases/artifacts"
ZIP_PATH="$ARTIFACT_DIR/AcademicTracker-source-$VERSION.zip"

echo "Cleaning generated files before source archive..."
"$ROOT_DIR/scripts/clean-release.sh"

mkdir -p "$ARTIFACT_DIR"
rm -f "$ZIP_PATH"

echo "Creating source release archive: $ZIP_PATH"
zip -r "$ZIP_PATH" . \
  -x "./.git/*" \
  -x "./target/*" \
  -x "./*/target/*" \
  -x "./dist/*" \
  -x "./releases/artifacts/*" \
  -x "./.DS_Store" \
  -x "./__MACOSX/*"

echo "Source release archive created:"
echo "$ZIP_PATH"
