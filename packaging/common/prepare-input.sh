#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
cd "$ROOT_DIR"

# shellcheck disable=SC1091
source "$ROOT_DIR/packaging/common/package-config.env"

APP_VERSION="${APP_VERSION_OVERRIDE:-$APP_VERSION}"
INPUT_DIR="$ROOT_DIR/$BUILD_DIR"
METADATA_FILE="$ROOT_DIR/target/package-metadata.env"

if ! command -v java >/dev/null 2>&1; then
  echo "Java 21 is required but java was not found on PATH." >&2
  exit 1
fi

if ! command -v javac >/dev/null 2>&1; then
  echo "A full Java 21 JDK is required, but javac was not found on PATH." >&2
  exit 1
fi

JAVA_SPEC="$(java -XshowSettings:properties -version 2>&1 | awk '/java.specification.version/ {print $3; exit}')"
JAVA_MAJOR="${JAVA_SPEC%%.*}"
if [ "${JAVA_MAJOR:-0}" -lt 21 ]; then
  echo "Java 21 or newer is required. Current java version output:" >&2
  java -version >&2
  exit 1
fi

if [ ! -x "$ROOT_DIR/mvnw" ]; then
  echo "Maven wrapper is missing or not executable: $ROOT_DIR/mvnw" >&2
  exit 1
fi

echo "Preparing jpackage input for $APP_NAME $APP_VERSION"
echo "Building Maven artifacts..."
./mvnw -pl "$APP_MODULE" -am clean install -DskipTests

echo "Creating clean package input directory: $INPUT_DIR"
rm -rf "$INPUT_DIR"
mkdir -p "$INPUT_DIR"
mkdir -p "$(dirname "$METADATA_FILE")"

echo "Copying runtime dependencies..."
./mvnw -pl "$APP_MODULE" dependency:copy-dependencies \
  -DincludeScope=runtime \
  -DoutputDirectory="$INPUT_DIR"

MAIN_JAR="$(find "$ROOT_DIR/$APP_MODULE/target" -maxdepth 1 -type f \
  -name "${APP_ARTIFACT_PREFIX}-*.jar" \
  ! -name "*-sources.jar" \
  ! -name "*-javadoc.jar" \
  | sort | head -n 1)"

if [ -z "$MAIN_JAR" ] || [ ! -s "$MAIN_JAR" ]; then
  echo "Could not find a non-empty app jar under $APP_MODULE/target." >&2
  exit 1
fi

cp "$MAIN_JAR" "$INPUT_DIR/"

cat > "$METADATA_FILE" <<EOF
PACKAGE_INPUT_DIR=$(printf '%q' "$INPUT_DIR")
PACKAGE_MAIN_JAR=$(printf '%q' "$(basename "$MAIN_JAR")")
PACKAGE_VERSION=$(printf '%q' "$APP_VERSION")
PACKAGE_APP_NAME=$(printf '%q' "$APP_NAME")
EOF

echo "Package input ready."
echo "Input dir: $INPUT_DIR"
echo "Main jar:  $(basename "$MAIN_JAR")"
echo "Metadata:  $METADATA_FILE"
