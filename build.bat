@echo off
setlocal enabledelayedexpansion

rem Save the project root so paths work even after cd
set ROOT=%CD%

set GSON=%ROOT%\lib\gson-2.10.1.jar
rem ============================================================
rem build.bat - Compile and package the Connections Game (Windows)
rem Usage: build.bat
rem ============================================================
setlocal enabledelayedexpansion

rem Verify prerequisites
where javac >nul 2>&1
if %errorlevel% neq 0 (
    echo ERROR: javac not found. Please install Java 21+.
    exit /b 1
)
if not exist "%GSON%" (
    echo ERROR: %GSON% not found. Place gson-2.10.1.jar inside lib\
    exit /b 1
)

if not exist build\net    mkdir build\net
if not exist build\server mkdir build\server
if not exist build\client mkdir build\client
if not exist dist         mkdir dist

rem Step 1: compile net (must be first - server and client depend on it)
echo [1/4] Compiling net...
dir /s /b net\src\main\java\*.java > _sources.txt
javac --release 21 -cp "%GSON%" -d build\net @_sources.txt
del _sources.txt

if %errorlevel% neq 0 (
    echo BUILD FAILED.
    exit /b 1
)

rem Step 2: compile server
echo [2/4] Compiling server...
dir /s /b server\src\main\java\*.java > _sources.txt
javac --release 21 -cp "%GSON%;build\net" -d build\server @_sources.txt
del _sources.txt

if %errorlevel% neq 0 (
    echo BUILD FAILED.
    exit /b 1
)

rem Step 3: compile client
echo [3/4] Compiling client...
dir /s /b client\src\main\java\*.java > _sources.txt
javac --release 21 -cp "%GSON%;build\net" -d build\client @_sources.txt
del _sources.txt

if %errorlevel% neq 0 (
    echo BUILD FAILED.
    exit /b 1
)

rem Step 4: package fat JARs
echo [4/4] Packaging JARs...

rem -- server.jar --
mkdir dist\_tmp_server
cd dist\_tmp_server
jar xf "%GSON%"
xcopy /s /q "%ROOT%\build\net\*" . >nul
xcopy /s /q "%ROOT%\build\server\*" . >nul
(echo Manifest-Version: 1.0& echo Main-Class: com.connectionsgame.server.ServerMain& echo.) > MANIFEST.MF
jar cfm "%ROOT%\dist\server.jar" MANIFEST.MF .
cd "%ROOT%"
rmdir /s /q dist\_tmp_server

rem -- client.jar --
mkdir dist\_tmp_client
cd dist\_tmp_client
jar xf "%GSON%"
xcopy /s /q "%ROOT%\build\net\*" . >nul
xcopy /s /q "%ROOT%\build\client\*" . >nul
(echo Manifest-Version: 1.0& echo Main-Class: com.connectionsgame.client.ClientMain& echo.) > MANIFEST.MF
jar cfm "%ROOT%\dist\client.jar" MANIFEST.MF .
cd "%ROOT%"
rmdir /s /q dist\_tmp_client

echo.
echo Build successful!
echo   dist\server.jar
echo   dist\client.jar
endlocal