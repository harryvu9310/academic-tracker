# Linux Packaging

Build on Linux with a full Java 21 JDK:

```bash
./packaging/linux/package-linux.sh
```

Output can include:

```text
dist/linux/AcademicTracker-1.0.0-linux-x64.deb
dist/linux/AcademicTracker-1.0.0-linux-x64.rpm
dist/linux/AcademicTracker-1.0.0-linux-x64.tar.gz
```

The script always creates a `.tar.gz` app-image fallback. It attempts:

- DEB if `dpkg-deb` is available
- RPM if `rpmbuild` is available

Optional icon:

```text
packaging/icons/app.png
```

Users running the tarball may need to mark the launcher executable after extraction.
