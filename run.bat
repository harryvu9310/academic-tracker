@echo off
setlocal
cd /d "%~dp0"
where java >nul 2>nul
if errorlevel 1 (
  echo Java 21 is required but java was not found on PATH.
  exit /b 1
)

call mvnw.cmd -pl app -am javafx:run
