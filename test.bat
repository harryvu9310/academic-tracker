@echo off
setlocal
cd /d "%~dp0"
call mvnw.cmd test
if errorlevel 1 exit /b 1
call mvnw.cmd -pl app -am test
