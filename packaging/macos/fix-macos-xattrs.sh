#!/usr/bin/env bash
set -euo pipefail

APP_PATH="${1:-}"

if [ -z "$APP_PATH" ]; then
  echo "Usage: $0 '/path/to/Academic Tracker.app'" >&2
  exit 1
fi

if [ "$(uname -s)" != "Darwin" ]; then
  echo "macOS extended attribute cleanup only runs on macOS. Current OS: $(uname -s)" >&2
  exit 1
fi

if [ ! -d "$APP_PATH" ]; then
  echo "App path does not exist: $APP_PATH" >&2
  exit 1
fi

echo "Cleaning macOS extended attributes for: $APP_PATH"
chflags -R nouchg "$APP_PATH" 2>/dev/null || true
xattr -cr "$APP_PATH" 2>/dev/null || true
find "$APP_PATH" -exec xattr -c {} + 2>/dev/null || true

if xattr -lr "$APP_PATH" 2>/dev/null | grep -E "com\\.apple\\.(FinderInfo|ResourceFork)" >/dev/null; then
  echo "FinderInfo or ResourceFork metadata remains after cleanup:" >&2
  xattr -lr "$APP_PATH" 2>/dev/null | grep -E "com\\.apple\\.(FinderInfo|ResourceFork)" >&2
  exit 1
fi

if ! command -v codesign >/dev/null 2>&1; then
  echo "codesign was not found. A full macOS developer tools environment is required." >&2
  exit 1
fi

echo "Applying ad-hoc local testing signature..."
codesign -s - --force --deep --verbose=4 "$APP_PATH"

echo "Verifying app signature..."
codesign --verify --deep --strict --verbose=4 "$APP_PATH"

echo "macOS app cleanup/signature verification complete."
