#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

echo "== Academic Tracker Doctor =="
echo "Project root: $ROOT_DIR"
echo "OS: $(uname -s) $(uname -m)"

if command -v java >/dev/null 2>&1; then
  echo "Java: $(java -version 2>&1 | head -n 1)"
else
  echo "Java: missing"
fi

if command -v javac >/dev/null 2>&1; then
  echo "javac: $(javac -version 2>&1)"
else
  echo "javac: missing"
fi

if command -v jpackage >/dev/null 2>&1; then
  echo "jpackage: available"
else
  echo "jpackage: missing (needed for native packages)"
fi

if [ -x ./mvnw ]; then
  echo "Maven wrapper: executable"
else
  echo "Maven wrapper: missing or not executable"
fi

echo "Checking required UI resources..."
for resource in \
  "app/src/main/resources/styles/common.css" \
  "app/src/main/resources/styles/apple-light.css" \
  "app/src/main/resources/styles/apple-dark.css" \
  "app/src/main/resources/com/tracker/academictracker/Dashboard.fxml" \
  "app/src/main/resources/com/tracker/academictracker/CourseRoster.fxml" \
  "app/src/main/resources/com/tracker/academictracker/CourseDetails.fxml" \
  "app/src/main/resources/com/tracker/academictracker/Semesters.fxml" \
  "app/src/main/resources/com/tracker/academictracker/Settings.fxml" \
  "app/src/main/resources/com/tracker/academictracker/Welcome.fxml"; do
  if [ -f "$resource" ]; then
    echo "ok: $resource"
  else
    echo "missing: $resource"
  fi
done

if [ "$(uname -s)" = "Linux" ] && [ -z "${DISPLAY:-}" ] && [ -z "${WAYLAND_DISPLAY:-}" ]; then
  echo "GUI display: not detected. JavaFX windows need DISPLAY, Wayland, VNC, or a desktop session."
else
  echo "GUI display: appears available or not required on this OS."
fi

echo "Doctor check complete."
