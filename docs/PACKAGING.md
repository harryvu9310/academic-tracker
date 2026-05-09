# Packaging

Packaging uses Java 21 `jpackage`. Native packages are platform-specific, so build each package on its own OS or on matching CI runners.

## Platform Rule

- macOS packages must be built on macOS.
- Windows packages must be built on Windows.
- Linux packages must be built on Linux.

One OS cannot reliably create native installers for every other OS.

## Common Configuration

Shared packaging values live in:

```text
packaging/common/package-config.env
```

The shared input-preparation script is:

```bash
./packaging/common/prepare-input.sh
```

It builds the Maven reactor, copies the app jar plus runtime dependencies, and writes packaging metadata under `target/package-input/`.

## macOS

```bash
./packaging/macos/package-macos.sh
```

Output:

```text
dist/macos/app-image/Academic Tracker.app
dist/macos/dmg/AcademicTracker-1.0.0-macos-arm64.dmg
dist/macos/dmg/AcademicTracker-1.0.0-macos-x64.dmg
```

The exact architecture suffix depends on `uname -m`.

The macOS script:

- runs only on macOS
- creates `.app`
- cleans extended attributes
- applies an ad-hoc local testing signature
- creates `.dmg`
- falls back to `hdiutil` if jpackage DMG creation hits signing/xattr problems

Optional icon:

```text
packaging/icons/app.icns
```

## Windows

```bat
packaging\windows\package-windows.bat
```

or:

```powershell
powershell -ExecutionPolicy Bypass -File .\packaging\windows\package-windows.ps1
```

Output:

```text
dist/windows/AcademicTracker-1.0.0-windows-x64.exe
```

Optional MSI:

```powershell
$env:CREATE_MSI="1"
.\packaging\windows\package-windows.ps1
```

MSI generation may require WiX Toolset. If MSI fails, the script keeps the EXE result when it exists.

Optional icon:

```text
packaging/icons/app.ico
```

## Linux

```bash
./packaging/linux/package-linux.sh
```

Output can include:

```text
dist/linux/AcademicTracker-1.0.0-linux-x64.deb
dist/linux/AcademicTracker-1.0.0-linux-x64.rpm
dist/linux/AcademicTracker-1.0.0-linux-x64.tar.gz
```

The Linux script always creates an app-image tarball fallback. It attempts DEB when `dpkg-deb` exists and RPM when `rpmbuild` exists.

Optional icon:

```text
packaging/icons/app.png
```

## Signing And Public Distribution

Local packages are suitable for testing and sharing with trusted friends.

- macOS public distribution requires Apple Developer signing and notarization.
- Windows public distribution requires a code-signing certificate to reduce SmartScreen warnings.
- Linux package trust depends on your distribution channel and repository signing.

## Cleanup

Generated artifacts are ignored by Git. Clean them with:

```bash
./scripts/clean-release.sh
```

## GitHub Releases

Build artifacts under `dist/` can be uploaded to GitHub Releases or shared through another trusted file host. See `releases/README.md`.
