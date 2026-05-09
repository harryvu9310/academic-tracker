# Packaging

This folder contains platform-specific Java 21 `jpackage` scripts.

```text
packaging/
  common/      shared package config and input preparation
  macos/       macOS .app/.dmg packaging
  windows/     Windows .exe/.msi packaging
  linux/       Linux app-image/.deb/.rpm/.tar.gz packaging
  icons/       optional platform icons
```

Native packages must be built on their target OS:

- macOS on macOS
- Windows on Windows
- Linux on Linux

Run tests before packaging:

```bash
./scripts/verify.sh
```

Commands:

```bash
./packaging/macos/package-macos.sh
./packaging/linux/package-linux.sh
```

Windows:

```bat
packaging\windows\package-windows.bat
```

Generated output goes to `dist/`, which is ignored by Git.

Backward-compatible wrapper scripts remain at:

```text
packaging/package-macos.sh
packaging/package-linux.sh
packaging/package-windows.ps1
```
