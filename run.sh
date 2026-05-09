#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$ROOT_DIR"

if [ -f "$HOME/.academic-tracker-env" ]; then
  # Keep VS Code terminal and plain terminal on the same Java runtime.
  . "$HOME/.academic-tracker-env"
fi

if ! command -v java >/dev/null 2>&1; then
  echo "Java 21 is required but java was not found on PATH." >&2
  exit 1
fi

if [ "$(uname -s)" = "Linux" ] && [ -z "${DISPLAY:-}" ] && [ -z "${WAYLAND_DISPLAY:-}" ]; then
  echo "Warning: JavaFX requires a GUI DISPLAY. If you see 'Unable to open DISPLAY', run on a desktop environment or configure X11 forwarding." >&2
fi

echo "Starting Academic Tracker..."
./mvnw -pl app -am javafx:run
