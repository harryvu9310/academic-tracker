# Academic Tracker

Academic Tracker is a Java 21 + JavaFX desktop app for managing semesters, courses, assessments, GPA progress, and deterministic GPA target prediction.

The project is a Maven multi-module application:

- `app`: JavaFX desktop UI, persistence, validation, app models, and prediction mapping.
- `prediction-core`: reusable GPA calculation and prediction engine with no JavaFX dependency.

## Features

- Course, semester, assessment, credit, and major-course tracking.
- Official GPA based only on completed courses with valid finalized assessments.
- Current projected GPA and scenario GPA for active/planned academic planning.
- Cumulative and major GPA target prediction.
- Course-level target prediction with pass risk, required remaining average, and explainable recommendations.
- Apple-inspired JavaFX UI with light/dark themes and smoother scrolling/responsive layouts.
- Local JSON persistence with validation before save/import.
- Cross-platform Maven build/test/run workflow.
- Prepared `jpackage` scripts for macOS, Windows, and Linux.

## Folder Structure

```text
Tracking/
  app/                 JavaFX desktop application
  prediction-core/     UI-independent GPA and prediction engine
  docs/                Architecture, development, prediction, UI, release docs
  packaging/           Platform-specific jpackage scripts
  scripts/             Verify, doctor, cleanup, release zip helpers
  releases/            Notes for generated release artifacts
  pom.xml              Maven multi-module root
  mvnw, mvnw.cmd       Maven wrapper
  run.sh, run.bat      App launchers
  test.sh, test.bat    Test launchers
```

Generated output goes to `target/`, `dist/`, or `releases/artifacts/`; those paths are ignored by Git.

## Requirements

- Java 21 JDK.
- Maven wrapper included in this repository.
- JavaFX dependencies and JavaFX Maven plugin are handled by Maven.

## Test

```bash
./scripts/verify.sh
```

Equivalent commands:

```bash
./mvnw test
./mvnw -pl app -am test
```

Windows:

```bat
test.bat
```

## Run

Linux/macOS:

```bash
./run.sh
```

Windows:

```bat
run.bat
```

Equivalent Maven command:

```bash
./mvnw -pl app -am javafx:run
```

JavaFX requires a desktop GUI session. On Linux servers, SSH sessions, or CI without display forwarding, `Unable to open DISPLAY` means the environment cannot open a JavaFX window.

## Data Storage

By default, the app stores data at:

```text
~/.academic-tracker/student_data.json
```

For safe QA, override the data file:

```bash
ACADEMIC_TRACKER_DATA_FILE="$(pwd)/tmp/student_data.json" ./run.sh
```

The app validates imported JSON before replacing the current save file.

## Packaging

Native packages are platform-specific:

- macOS packages must be built on macOS.
- Windows packages must be built on Windows.
- Linux packages must be built on Linux.

macOS:

```bash
./packaging/macos/package-macos.sh
```

Windows:

```bat
packaging\windows\package-windows.bat
```

Linux:

```bash
./packaging/linux/package-linux.sh
```

Legacy wrapper paths still work:

```bash
./packaging/package-macos.sh
./packaging/package-linux.sh
```

PowerShell wrapper:

```powershell
.\packaging\package-windows.ps1
```

Files to share with friends:

- macOS: `dist/macos/dmg/AcademicTracker-<version>-macos-<arch>.dmg`
- Windows: `dist/windows/AcademicTracker-<version>-windows-x64.exe`
- Linux: `dist/linux/AcademicTracker-<version>-linux-x64.deb`, `.rpm`, or `.tar.gz`

See [docs/PACKAGING.md](docs/PACKAGING.md).

## Signing Warnings

- macOS builds are ad-hoc signed for local testing, not notarized. Gatekeeper may require right-click Open or Privacy & Security Open Anyway.
- Windows installers are unsigned and may trigger SmartScreen. Public distribution requires a code-signing certificate.
- Linux tarballs may require executable permissions after extraction.

## Development

- VS Code: open the `Tracking` folder and use the included tasks.
- IntelliJ IDEA: open the Maven root folder and import the Maven project.
- Do not run individual Java files directly with a raw classpath; JavaFX and module dependencies are managed by Maven.

See [docs/DEVELOPMENT.md](docs/DEVELOPMENT.md).

## Troubleshooting

- `Permission denied`: run `chmod +x mvnw run.sh test.sh scripts/*.sh packaging/*.sh packaging/*/*.sh`.
- `jpackage not found`: install a full Java 21 JDK and ensure it is on `PATH`.
- Maven download failures usually indicate internet/DNS/proxy issues; rerun after connectivity is restored.
- Run `./scripts/doctor.sh` for a quick environment check.
