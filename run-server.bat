@echo off
rem ============================================================
rem run-server.bat - Start the Connections Game server (Windows)
rem Must be run from the project root (where server.properties is).
rem Build first with: build.bat
rem ============================================================

set JAR=dist\server.jar

if not exist "%JAR%" (
    echo ERROR: %JAR% not found. Run build.bat first.
    exit /b 1
)

java -jar "%JAR%"