# Architecture

Academic Tracker is split into two Maven modules.

## `prediction-core`

`prediction-core` is a reusable Java library. It has no JavaFX imports and can be tested or reused independently.

Main packages:

- `model`: prediction input models such as course, assessment, program config, status, and performance history.
- `core`: deterministic GPA conversion and grade calculation.
- `engine`: course prediction, global GPA prediction, strength profile, target recommendation, and priority ranking.
- `result`: immutable result types returned to the app.
- `util`: validation and math helpers.

## `app`

`app` is the JavaFX desktop application.

Main packages:

- `model`: app-side student, semester, course, and assessment data.
- `service`: persistence, input validation, data validation, prediction mapping, and app-facing prediction services.
- `controller`: JavaFX screen controllers.
- `ui`: theme and reusable UI helpers.
- `resources`: FXML screens and CSS themes.

## Data Flow

```text
JavaFX Controller
  -> InputValidator / StudentDataValidator
  -> AcademicPredictionMapper
  -> prediction-core engines
  -> result objects
  -> JavaFX UI cards, badges, and messages
```

Persistence flows through `DataManager`, which validates data before writing JSON to disk. Import uses preview validation before replacing the current save file.

## Design Boundaries

- Prediction logic does not live in JavaFX controllers.
- `prediction-core` must remain independent from JavaFX and app persistence.
- App controllers can format results for UI, but should not reimplement GPA formulas.
- Official GPA and projected/scenario GPA must remain conceptually separate.

## Release Structure

Packaging and release engineering files are separate from app code:

- `packaging/common`: shared package metadata and input preparation.
- `packaging/macos`: macOS `.app` and `.dmg` scripts.
- `packaging/windows`: Windows `.exe` and optional `.msi` scripts.
- `packaging/linux`: Linux app-image, DEB/RPM, and tarball scripts.
- `scripts`: local verification, doctor, cleanup, and source archive helpers.
- `releases`: documentation for generated artifacts; large binaries are not committed.
