#!/bin/bash
# ============================================================
# run-server.sh - Start the Connections Game server (Linux/macOS)
# Must be run from the project root (where server.properties is).
# Build first with: ./build.sh
# ============================================================

JAR="dist/server.jar"

if [ ! -f "$JAR" ]; then
    echo "ERROR: $JAR not found. Run ./build.sh first."
    exit 1
fi

java -jar "$JAR"