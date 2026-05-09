#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

echo "== Academic Tracker verification =="

if ! command -v java >/dev/null 2>&1; then
  echo "Java 21 is required but java was not found on PATH." >&2
  exit 1
fi

JAVA_VERSION_OUTPUT="$(java -version 2>&1 | head -n 1)"
echo "Java: $JAVA_VERSION_OUTPUT"
if ! java -version 2>&1 | grep -Eq 'version "21|openjdk version "21'; then
  echo "Warning: expected Java 21. Continuing, but JavaFX packaging/running may fail on other versions." >&2
fi

if [ ! -x "./mvnw" ]; then
  echo "Maven wrapper ./mvnw is missing or not executable." >&2
  exit 1
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
  if [ ! -f "$resource" ]; then
    echo "Missing required resource: $resource" >&2
    exit 1
  fi
done

echo "Running full Maven test suite..."
./mvnw test

echo "Running app module test suite with dependencies..."
./mvnw -pl app -am test

echo
echo "Verification complete."
echo "Next GUI check: ./run.sh"
echo "If JavaFX reports 'Unable to open DISPLAY', run from a desktop session or configure X11/Wayland forwarding."
