# Release Checklist

Use this before sharing builds with friends or publishing GitHub Releases.

## Source Health

```bash
./scripts/doctor.sh
./scripts/verify.sh
./mvnw clean test
./mvnw -pl app -am test
```

Check for generated junk:

```bash
find . -name ".DS_Store" -o -name "__MACOSX"
find . -path "*/target" -type d
```

Clean when needed:

```bash
./scripts/clean-release.sh
```

## Manual GUI QA

- Run `./run.sh` or `run.bat`.
- Resize the window vertically and horizontally.
- Verify Dashboard, Courses, Course Details, Semesters, Settings, and Welcome remain usable.
- Verify long pages scroll vertically.
- Verify wide tables expose horizontal scrolling instead of clipping important columns.
- Verify light and dark themes.
- Verify GPA/prediction wording avoids certainty claims.

## Package Per OS

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

## Artifact Names

- macOS: `AcademicTracker-1.0.0-macos-<arch>.dmg`
- Windows: `AcademicTracker-1.0.0-windows-x64.exe`
- Linux: `AcademicTracker-1.0.0-linux-x64.deb`, `.rpm`, or `.tar.gz`

## Signing

- macOS public distribution requires Developer ID signing and notarization.
- Windows public distribution requires a code-signing certificate.
- Linux distribution should use trusted package channels or checksums.

## Source Zip

```bash
./scripts/create-release-zip.sh 1.0.0
```

The source zip is written under `releases/artifacts/`, which is ignored by Git.
