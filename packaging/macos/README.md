# macOS Packaging

Build on macOS with a full Java 21 JDK:

```bash
./packaging/macos/package-macos.sh
```

Output:

```text
dist/macos/app-image/Academic Tracker.app
dist/macos/dmg/AcademicTracker-1.0.0-macos-<arch>.dmg
```

`<arch>` is detected from `uname -m` and is usually `arm64` on Apple Silicon or `x64` on Intel Macs.

Optional icon:

```text
packaging/icons/app.icns
```

The script clears macOS extended attributes and applies an ad-hoc local testing signature. This is not Apple notarization. For public distribution, configure Developer ID signing and notarization separately.

If Gatekeeper blocks a local unsigned/ad-hoc signed build, right-click Open or use Privacy & Security Open Anyway only for builds you trust.
