# Releases

Generated release artifacts should be placed under `dist/` by the platform packaging scripts, then uploaded to GitHub Releases or shared through a trusted file host.

Do not commit large binaries to this folder by default.

Recommended files to share:

- macOS: `dist/macos/dmg/AcademicTracker-<version>-macos-<arch>.dmg`
- Windows: `dist/windows/AcademicTracker-<version>-windows-x64.exe`
- Linux: `dist/linux/AcademicTracker-<version>-linux-x64.deb`, `.rpm`, or `.tar.gz`

`releases/artifacts/` is ignored and can be used locally for generated source zips or copied installers.
