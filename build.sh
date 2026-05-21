#!/bin/bash
# ============================================================
# build.sh - Compile and package the Connections Game (Linux/macOS)
# Usage: chmod +x build.sh && ./build.sh
# ============================================================
set -e

GSON="lib/gson-2.10.1.jar"

# Verify prerequisites
if ! command -v javac &> /dev/null; then
    echo "ERROR: javac not found. Please install Java 21+."
    exit 1
fi
if [ ! -f "$GSON" ]; then
    echo "ERROR: $GSON not found. Place gson-2.10.1.jar inside lib/"
    exit 1
fi

mkdir -p build/net build/server build/client dist

# Step 1: compile net (must be first - server and client depend on it)
echo "[1/4] Compiling net..."
find net/src/main/java -name "*.java" | xargs javac --release 21 \
    -cp "$GSON" -d build/net

# Step 2: compile server
echo "[2/4] Compiling server..."
find server/src/main/java -name "*.java" | xargs javac --release 21 \
    -cp "$GSON:build/net" -d build/server

# Step 3: compile client
echo "[3/4] Compiling client..."
find client/src/main/java -name "*.java" | xargs javac --release 21 \
    -cp "$GSON:build/net" -d build/client

# Step 4: package fat JARs
# A fat JAR bundles all classes + Gson so it runs with just: java -jar
echo "[4/4] Packaging JARs..."

# -- server.jar --
mkdir -p dist/_tmp_server && cd dist/_tmp_server
jar xf "../../$GSON"
cp -r "../../build/net/." . && cp -r "../../build/server/." .
printf "Manifest-Version: 1.0\nMain-Class: com.connectionsgame.server.ServerMain\n" > MANIFEST.MF
jar cfm ../server.jar MANIFEST.MF .
cd ../.. && rm -rf dist/_tmp_server

# -- client.jar --
mkdir -p dist/_tmp_client && cd dist/_tmp_client
jar xf "../../$GSON"
cp -r "../../build/net/." . && cp -r "../../build/client/." .
printf "Manifest-Version: 1.0\nMain-Class: com.connectionsgame.client.ClientMain\n" > MANIFEST.MF
jar cfm ../client.jar MANIFEST.MF .
cd ../.. && rm -rf dist/_tmp_client

echo ""
echo "Build successful!"
echo "  dist/server.jar"
echo "  dist/client.jar"