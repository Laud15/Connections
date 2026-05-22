#!/bin/bash
# ============================================================
# build.sh - Build Connections Game (Linux/macOS)
# ============================================================

set -e

GSON="lib/gson-2.10.1.jar"

echo "========================================="
echo " Building Connections Game"
echo "========================================="

# Check Java
if ! command -v javac &> /dev/null; then
    echo "ERROR: javac not found. Install Java 21+."
    exit 1
fi

# Check Gson
if [ ! -f "$GSON" ]; then
    echo "ERROR: $GSON not found."
    exit 1
fi

# Clean old build
rm -rf build dist

mkdir -p build dist

echo "[1/2] Compiling sources..."

# Collect all java files
find net/src/main/java server/src/main/java client/src/main/java -name "*.java" > sources.txt

# Compile everything together
javac --release 21 -cp "$GSON" -d build @sources.txt

rm sources.txt

echo "[2/2] Packaging JARs..."

# Server JAR
jar cfe dist/server.jar com.connectionsgame.server.ServerMain -C build .

# Client JAR
jar cfe dist/client.jar com.connectionsgame.client.ClientMain -C build .

echo ""
echo "Build successful!"
echo "  dist/server.jar"
echo "  dist/client.jar"