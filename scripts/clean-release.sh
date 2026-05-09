#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

echo "Cleaning generated build artifacts and OS junk from: $ROOT_DIR"

echo "- Removing Maven target directories"
find . -type d -name target -prune -print -exec rm -rf {} +

echo "- Removing macOS metadata files"
find . -name ".DS_Store" -print -delete
find . -type d -name "__MACOSX" -prune -print -exec rm -rf {} +

echo "- Removing transient backup/temp files"
find . \( -name "*.tmp" -o -name "*.bak" -o -name "*.log" -o -name "*.class" \) -print -delete
find . -name "package-log.txt" -print -delete

echo "- Removing generated native package output"
find . -maxdepth 1 -type d -name "dist" -print -exec rm -rf {} +
find releases -type d -name "artifacts" -prune -print -exec rm -rf {} + 2>/dev/null || true
find . -maxdepth 2 \( -name "*.dmg" -o -name "*.exe" -o -name "*.msi" -o -name "*.deb" -o -name "*.rpm" -o -name "*.zip" \) -print -delete

echo "Clean release workspace is ready."
