# Windows Packaging

Build on Windows with a full Java 21 JDK:

```bat
packaging\windows\package-windows.bat
```

PowerShell:

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

MSI creation may require WiX Toolset. The EXE is the primary Windows artifact.

Optional icon:

```text
packaging/icons/app.ico
```

Unsigned Windows installers can trigger SmartScreen. Public distribution should use a code-signing certificate.
