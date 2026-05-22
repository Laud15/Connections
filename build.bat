@echo off
setlocal

set GSON=lib\gson-2.10.1.jar

if exist build rmdir /s /q build
if exist dist rmdir /s /q dist

mkdir build
mkdir dist

echo Compiling...

dir /s /b net\src\main\java\*.java > sources.txt
dir /s /b server\src\main\java\*.java >> sources.txt
dir /s /b client\src\main\java\*.java >> sources.txt

javac --release 21 -cp "%GSON%" -d build @sources.txt

del sources.txt

if %errorlevel% neq 0 (
  echo BUILD FAILED
  exit /b 1
)

echo Creating jars...

jar cfe dist\server.jar com.connectionsgame.server.ServerMain -C build .
jar cfe dist\client.jar com.connectionsgame.client.ClientMain -C build .

echo Done.