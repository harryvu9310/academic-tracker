#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$ROOT_DIR"

if [ -f "$HOME/.academic-tracker-env" ]; then
  # Keep VS Code terminal and plain terminal on the same Java runtime.
  . "$HOME/.academic-tracker-env"
fi

./scripts/verify.sh
