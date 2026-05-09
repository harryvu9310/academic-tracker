#!/usr/bin/env bash
set -uo pipefail

ROOT_DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$ROOT_DIR"

if ! ./run.sh; then
  echo
  echo "Academic Tracker failed to start. Review the message above."
  echo "On macOS, make sure Java 21 is installed and run this from a desktop session."
  read -r -p "Press Enter to close this window..."
  exit 1
fi
