# Changelog

## 1.0.0-prepackage

- Hardened GPA and prediction validation against invalid numeric values.
- Kept official GPA separate from projected and scenario GPA.
- Excluded incomplete completed courses from official GPA, completed credits, and performance history.
- Added UI warnings for completed courses that still need final assessment data.
- Added light/dark theme resources and resource availability tests.
- Improved save failure handling so persistence errors are shown instead of silently swallowed.
- Cleaned Maven configuration with centralized versions and plugin management.
- Added release cleanup and verification scripts.
- Added starter `jpackage` scripts for Windows, macOS, and Linux.
- Upgraded macOS packaging script to generate both `.app` and `.dmg` outputs with optional `.icns` support.
- Reorganized packaging into common, macOS, Windows, Linux, and icons folders.
- Added platform-specific packaging docs, release checklist, doctor script, and source zip helper.
- Added macOS xattr cleanup/ad-hoc signing helper for local testing builds.
- Rewrote project README and added architecture, development, prediction, packaging, and UI docs.
