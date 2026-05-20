#!/bin/bash
# ============================================================
# compile.sh
#
# Compiles all three modules (net, server, client) from source
# using javac directly, as required by the project specification.
#
# Prerequisites:
#   - Java 21+ (javac and jar) on PATH
#   - lib/gson-2.10.1.jar present
#
# Usage:
#   chmod +x compile.sh
#   ./compile.sh
#
# On Windows use ';' instead of ':' as classpath separator.
# ============================================================

set -e  # stop immediately on any error

GSON="lib/gson-2.10.1.jar"
BUILD="build"
SRC_NET="net/src/main/java"
SRC_SERVER="server/src/main/java"
SRC_CLIENT="client/src/main/java"

# ── Verify prerequisites ──────────────────────────────────────────────────────

if ! command -v javac &> /dev/null; then
    echo "ERROR: javac not found. Please install Java 21+."
    exit 1
fi

if [ ! -f "$GSON" ]; then
    echo "ERROR: $GSON not found."
    echo "Download it from:"
    echo "  https://repo1.maven.org/maven2/com/google/code/gson/gson/2.10.1/gson-2.10.1.jar"
    exit 1
fi

echo "========================================="
echo " Connections Game - Build"
echo "========================================="

# Create output directories
mkdir -p "$BUILD/net" "$BUILD/server" "$BUILD/client" dist

# ── Step 1: compile net module ────────────────────────────────────────────────
# Must be compiled first: both server and client depend on it.
echo "[1/4] Compiling net module..."
find "$SRC_NET" -name "*.java" | xargs javac \
    --release 21 \
    -cp "$GSON" \
    -d "$BUILD/net"
echo "      Done."

# ── Step 2: compile server module ─────────────────────────────────────────────
echo "[2/4] Compiling server module..."
find "$SRC_SERVER" -name "*.java" | xargs javac \
    --release 21 \
    -cp "$GSON:$BUILD/net" \
    -d "$BUILD/server"
echo "      Done."

# ── Step 3: compile client module ─────────────────────────────────────────────
echo "[3/4] Compiling client module..."
find "$SRC_CLIENT" -name "*.java" | xargs javac \
    --release 21 \
    -cp "$GSON:$BUILD/net" \
    -d "$BUILD/client"
echo "      Done."

# ── Step 4: package fat JARs ──────────────────────────────────────────────────
# A fat JAR bundles compiled classes + net module + Gson into a single
# self-contained executable file: java -jar dist/server.jar
echo "[4/4] Packaging JARs..."

# -- server.jar ----------------------------------------------------------------
mkdir -p dist/_tmp_server
cd dist/_tmp_server
jar xf "../../$GSON"                # unpack Gson classes into the temp folder
cp -r "../../$BUILD/net/." .        # add shared net module classes
cp -r "../../$BUILD/server/." .     # add server classes
mkdir -p META-INF
# MANIFEST.MF tells the JVM which class contains the main() method
printf "Manifest-Version: 1.0\nMain-Class: com.connectionsgame.server.ServerMain\n" \
    > META-INF/MANIFEST.MF
jar cfm "../server.jar" META-INF/MANIFEST.MF .
cd ../..
rm -rf dist/_tmp_server
echo "      dist/server.jar created."

# -- client.jar ----------------------------------------------------------------
mkdir -p dist/_tmp_client
cd dist/_tmp_client
jar xf "../../$GSON"
cp -r "../../$BUILD/net/." .
cp -r "../../$BUILD/client/." .
mkdir -p META-INF
printf "Manifest-Version: 1.0\nMain-Class: com.connectionsgame.client.ClientMain\n" \
    > META-INF/MANIFEST.MF
jar cfm "../client.jar" META-INF/MANIFEST.MF .
cd ../..
rm -rf dist/_tmp_client
echo "      dist/client.jar created."

echo ""
echo "========================================="
echo " Build successful!"
echo ""
echo "  dist/server.jar"
echo "  dist/client.jar"
echo ""
echo " Run from the project root:"
echo "   java -jar dist/server.jar"
echo "   java -jar dist/client.jar"
echo "========================================="