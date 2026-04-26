#!/bin/bash
# ============================================================
# compile.sh  -  Build script for Connections Game
#
# Compiles all three modules (net, server, client) from source
# using javac directly, as required by the project specification.
#
# Prerequisites:
#   - Java 21+ (javac) on PATH
#   - lib/gson-2.10.1.jar present
#     Download from: https://repo1.maven.org/maven2/com/google/code/gson/gson/2.10.1/gson-2.10.1.jar
#
# Usage:
#   chmod +x compile.sh
#   ./compile.sh
# ============================================================

set -e  # stop on first error

GSON="lib/gson-2.10.1.jar"
BUILD="build"
SRC_NET="net/src/main/java"
SRC_SERVER="server/src/main/java"
SRC_CLIENT="client/src/main/java"

# Verify Gson is present
if [ ! -f "$GSON" ]; then
    echo "ERROR: $GSON not found."
    echo "Download it with:"
    echo "  curl -L https://repo1.maven.org/maven2/com/google/code/gson/gson/2.10.1/gson-2.10.1.jar -o $GSON"
    exit 1
fi

echo "========================================="
echo " Connections Game - Build"
echo "========================================="

# ── 1. net module (shared types: requests, responses, MessageParser) ──────
echo "[1/3] Compiling net module..."
mkdir -p "$BUILD/net"
find "$SRC_NET" -name "*.java" | xargs javac \
    --release 21 \
    -cp "$GSON" \
    -d "$BUILD/net"
echo "      Done."

# ── 2. server module ──────────────────────────────────────────────────────
echo "[2/3] Compiling server module..."
mkdir -p "$BUILD/server"
find "$SRC_SERVER" -name "*.java" | xargs javac \
    --release 21 \
    -cp "$GSON:$BUILD/net" \
    -d "$BUILD/server"
echo "      Done."

# ── 3. client module ──────────────────────────────────────────────────────
echo "[3/3] Compiling client module..."
mkdir -p "$BUILD/client"
find "$SRC_CLIENT" -name "*.java" | xargs javac \
    --release 21 \
    -cp "$GSON:$BUILD/net" \
    -d "$BUILD/client"
echo "      Done."

# ── 4. Package JARs ───────────────────────────────────────────────────────
echo "[4/4] Packaging JARs..."

# server.jar  (fat jar: server classes + net classes + gson)
mkdir -p dist
cd dist

mkdir -p _tmp_server
cd _tmp_server
jar xf "../../$GSON"
cp -r "../../$BUILD/net/." .
cp -r "../../$BUILD/server/." .
# Write MANIFEST for server
mkdir -p META-INF
cat > META-INF/MANIFEST.MF << 'MANIFEST'
Manifest-Version: 1.0
Main-Class: com.connectionsgame.server.ServerMain
MANIFEST
jar cfm "../server.jar" META-INF/MANIFEST.MF .
cd ..
rm -rf _tmp_server

# client.jar  (fat jar: client classes + net classes + gson)
mkdir -p _tmp_client
cd _tmp_client
jar xf "../../$GSON"
cp -r "../../$BUILD/net/." .
cp -r "../../$BUILD/client/." .
mkdir -p META-INF
cat > META-INF/MANIFEST.MF << 'MANIFEST'
Manifest-Version: 1.0
Main-Class: com.connectionsgame.client.ClientMain
MANIFEST
jar cfm "../client.jar" META-INF/MANIFEST.MF .
cd ..
rm -rf _tmp_client

cd ..

echo ""
echo "========================================="
echo " Build successful!"
echo "  dist/server.jar"
echo "  dist/client.jar"
echo ""
echo " Run the server (from project root):"
echo "   java -jar dist/server.jar"
echo ""
echo " Run the client (from project root):"
echo "   java -jar dist/client.jar"
echo "========================================="
