# Development

## Setup

Install a Java 21 JDK. Maven itself does not need to be installed globally because the Maven wrapper is included.

Check Java:

```bash
java -version
```

## Build And Test

```bash
./mvnw test
./mvnw -pl app -am test
./scripts/verify.sh
```

Quick environment check:

```bash
./scripts/doctor.sh
```

## Run The App

```bash
./run.sh
```

Or directly:

```bash
./mvnw -pl app -am javafx:run
```

If JavaFX reports `Unable to open DISPLAY`, run from a desktop environment or configure display forwarding. This is an environment limitation, not a GPA/prediction failure.

## VS Code

Open the `Tracking` folder. The included VS Code tasks call:

- `./run.sh`
- `./test.sh`

Avoid running `Launcher.java` directly from a raw editor classpath because that bypasses Maven JavaFX dependencies.

Intentional VS Code config files are kept in `.vscode/`. Transient VS Code files remain ignored.

## IntelliJ IDEA

Open the `Tracking` folder as a Maven project. Use Maven reload after POM changes.

The `.run/` folder contains lightweight IntelliJ run configurations for launching and testing the app.

Recommended run command:

```text
-pl app -am javafx:run
```

Recommended test command:

```text
test
```

## Data File Override

For safe manual QA without touching real data:

```bash
ACADEMIC_TRACKER_DATA_FILE="$(pwd)/tmp/student_data.json" ./run.sh
```

Do not commit local data files.

## Troubleshooting

- Missing JavaFX classes: run through Maven, not raw `java -cp`.
- Invalid import: the app validates imported JSON and should preserve the existing save file.
- Save failure: controllers should show a user-facing error instead of silently continuing.
- Permission denied: run `chmod +x mvnw run.sh test.sh scripts/*.sh packaging/*.sh packaging/*/*.sh`.
- Native packages must be built on their matching OS. Use GitHub Actions runners if you need all three platforms.
