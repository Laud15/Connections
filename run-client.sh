#!/bin/bash
# ============================================================
# run-client.sh - Start the Connections Game client (Linux/macOS)
# Must be run from the project root (where client.properties is).
# Build first with: ./build.sh
# ============================================================

JAR="dist/client.jar"

if [ ! -f "$JAR" ]; then
    echo "ERROR: $JAR not found. Run ./build.sh first."
    exit 1
fi

java -jar "$JAR"