@echo off
rem ============================================================
rem run-client.bat - Start the Connections Game client (Windows)
rem Must be run from the project root (where client.properties is).
rem Build first with: build.bat
rem ============================================================

set JAR=dist\client.jar

if not exist "%JAR%" (
    echo ERROR: %JAR% not found. Run build.bat first.
    exit /b 1
)

java -jar "%JAR%"