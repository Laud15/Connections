#!/bin/bash

# ══════════════════════════════════════════════════════════════════════════════
# Connections Game - Build and Run Script
#
# This script provides a complete automation solution for the Connections game
# on Linux/macOS systems.
#
# Usage:
#   ./compile.sh          - Show interactive menu
#   ./compile.sh build    - Build only (compile + package)
#   ./compile.sh server   - Build and run server
#   ./compile.sh client   - Build and run client
#   ./compile.sh all      - Build and run both (in background)
# ══════════════════════════════════════════════════════════════════════════════

set -e

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

# ──────────────────────────────────────────────────────────────────────────────
# FUNCTIONS
# ──────────────────────────────────────────────────────────────────────────────

function print_title() {
    echo -e "${CYAN}═══════════════════════════════════════════════════════════${NC}"
    echo -e "${CYAN}  $1${NC}"
    echo -e "${CYAN}═══════════════════════════════════════════════════════════${NC}"
}

function print_section() {
    echo -e "${YELLOW}► $1${NC}"
}

function print_success() {
    echo -e "${GREEN}✓ $1${NC}"
}

function print_error() {
    echo -e "${RED}✗ $1${NC}"
}

function print_info() {
    echo -e "${BLUE}ℹ $1${NC}"
}

function test_requirement() {
    local name=$1
    local command=$2

    if command -v $command &> /dev/null; then
        print_success "$name is installed"
        $command --version 2>/dev/null | head -1 | xargs print_info "  →" || true
        return 0
    else
        print_error "$name is NOT installed"
        return 1
    fi
}

function verify_requirements() {
    print_title "Verifying Requirements"

    local all_ok=true

    if ! test_requirement "Java" "java"; then
        all_ok=false
    fi

    if ! test_requirement "Maven" "mvn"; then
        all_ok=false
    fi

    if [ "$all_ok" = false ]; then
        echo ""
        echo -e "${YELLOW}Installation instructions:${NC}"
        echo "  Java 21: https://adoptium.net"
        echo "  Maven:   https://maven.apache.org/install.html"
        echo ""
        exit 1
    fi

    print_success "All requirements satisfied!"
    echo ""
}

function build_project() {
    print_title "Building Connections Game"

    print_section "Step 1: Clean artifacts"
    mvn clean > /dev/null 2>&1
    print_success "Cleaned"

    print_section "Step 2: Compile source"
    mvn compile -DskipTests > /dev/null 2>&1
    print_success "Compiled"

    print_section "Step 3: Package with dependencies"
    mvn package -DskipTests > /dev/null 2>&1
    print_success "Packaged"

    echo ""
    print_success "Build complete!"
    print_info "Server JAR: server/target/server-1.0-jar-with-dependencies.jar"
    print_info "Client JAR: client/target/client-1.0-jar-with-dependencies.jar"
    echo ""
}

function run_server() {
    print_title "Starting Connections Game SERVER"

    local jar="server/target/server-1.0-jar-with-dependencies.jar"

    if [ ! -f "$jar" ]; then
        print_error "Server JAR not found: $jar"
        print_info "Building first..."
        build_project
    fi

    print_info "Configuration: server.properties"
    print_info "Main Class:    com.connectionsgame.server.ServerMain"
    echo ""
    echo -e "${GREEN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
    echo -e "${GREEN}Server starting... (Press Ctrl+C to stop)${NC}"
    echo -e "${GREEN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
    echo ""

    java -jar "$jar"
}

function run_client() {
    print_title "Starting Connections Game CLIENT"

    local jar="client/target/client-1.0-jar-with-dependencies.jar"

    if [ ! -f "$jar" ]; then
        print_error "Client JAR not found: $jar"
        print_info "Building first..."
        build_project
    fi

    print_info "Configuration: client.properties"
    print_info "Main Class:    com.connectionsgame.client.ClientMain"
    echo ""

    # Parse config
    local server_host=$(grep "^server.host" client.properties | cut -d'=' -f2 | xargs)
    local server_port=$(grep "^server.tcp.port" client.properties | cut -d'=' -f2 | xargs)
    local local_port=$(grep "^local.udp.port" client.properties | cut -d'=' -f2 | xargs)

    print_info "Connecting to: $server_host : $server_port"
    print_info "Local UDP port: $local_port"
    echo ""
    echo -e "${GREEN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
    echo -e "${GREEN}Client starting...${NC}"
    echo -e "${GREEN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
    echo ""
    echo -e "${CYAN}Available commands:${NC}"
    echo "  help                    - Show all commands"
    echo "  register <name> <psw>   - Register new account"
    echo "  login <name> <psw>      - Login"
    echo "  logout                  - Logout"
    echo "  propose <w1> <w2> <w3> <w4> - Submit proposal"
    echo "  gameinfo [gameId]       - Get current game info"
    echo "  gamestats [gameId]      - Get game statistics"
    echo "  leaderboard [top=K|player=name]"
    echo "  mystats                 - Your personal statistics"
    echo "  quit                    - Exit client"
    echo ""

    java -jar "$jar"
}

function show_menu() {
    print_title "Connections Game - Main Menu"

    echo -e "${CYAN}[1] Build project (compile + package)${NC}"
    echo -e "${CYAN}[2] Run Server only${NC}"
    echo -e "${CYAN}[3] Run Client only${NC}"
    echo -e "${CYAN}[4] Build and run Server${NC}"
    echo -e "${CYAN}[5] Build and run Client${NC}"
    echo -e "${CYAN}[6] Verify requirements${NC}"
    echo -e "${CYAN}[7] Exit${NC}"
    echo ""

    read -p "Choose option (1-7): " choice

    case $choice in
        1)
            verify_requirements
            build_project
            show_menu
            ;;
        2)
            if [ ! -f "server/target/server-1.0-jar-with-dependencies.jar" ]; then
                print_error "Server JAR not found. Building first..."
                verify_requirements
                build_project
            fi
            run_server
            ;;
        3)
            if [ ! -f "client/target/client-1.0-jar-with-dependencies.jar" ]; then
                print_error "Client JAR not found. Building first..."
                verify_requirements
                build_project
            fi
            run_client
            ;;
        4)
            verify_requirements
            build_project
            run_server
            ;;
        5)
            verify_requirements
            build_project
            run_client
            ;;
        6)
            verify_requirements
            show_menu
            ;;
        7)
            echo -e "${GREEN}Goodbye!${NC}"
            exit 0
            ;;
        *)
            print_error "Invalid choice"
            sleep 1
            show_menu
            ;;
    esac
}

# ──────────────────────────────────────────────────────────────────────────────
# MAIN
# ──────────────────────────────────────────────────────────────────────────────

action="${1:-menu}"

case "$action" in
    menu)
        show_menu
        ;;
    build)
        verify_requirements
        build_project
        ;;
    server)
        verify_requirements
        run_server
        ;;
    client)
        verify_requirements
        run_client
        ;;
    all)
        verify_requirements
        build_project
        print_info "Starting server in background..."
        java -jar server/target/server-1.0-jar-with-dependencies.jar &
        SERVER_PID=$!
        sleep 3
        print_info "Starting client..."
        java -jar client/target/client-1.0-jar-with-dependencies.jar
        kill $SERVER_PID 2>/dev/null || true
        ;;
    *)
        echo "Usage: $0 [menu|build|server|client|all]"
        exit 1
        ;;
esac
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
