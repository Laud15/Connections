# Connections Game

A server-client implementation of the **Connections** word-grouping game, inspired by the New York Times puzzle. Built as a final project for *Networks and Laboratory — Laboratory Module III*, A.Y. 2025/26.

## Overview

Players are presented with 16 words and must group them into 4 thematic categories of 4 words each. The server manages a single global game that all connected players participate in simultaneously. A new game starts automatically when the current one ends.

## Project Structure

```
ConnectionsGame/
├── net/          # Shared module: requests, responses, deserializers
├── server/       # Game logic, NIO server, persistence
├── client/       # CLI interface, TCP connection, UDP listener
├── data/
│   ├── Connections_Data.json   # Puzzle database (912 puzzles)
│   └── persistent/             # Runtime data (generated automatically)
│       ├── users/              # One JSON file per registered user
│       └── games/              # One JSON file per completed game
├── lib/
│   └── gson-2.10.1.jar         # Only external dependency
├── server.properties           # Server configuration
├── client.properties           # Client configuration
└── compile.sh                  # Build script (javac)
```

## Technologies

- **Java 21**
- **NIO** — persistent TCP connection on the client side; Selector + ThreadPoolExecutor on the server side
- **UDP** — asynchronous notifications from server to clients (game over)
- **Gson 2.10.1** — JSON serialization/deserialization
- **SHA-256** — password hashing

## Architecture

### Server
- A single **Selector thread** handles all NIO connections (accept, read, write)
- A **ThreadPoolExecutor** (`core = CPU cores`, `max = CPU cores × 4`, `CallerRunsPolicy`) processes requests in parallel without ever writing directly to channels — responses are handed back to the Selector thread via a `ConcurrentLinkedQueue` and `selector.wakeup()`
- A **GameManager** protected by a `ReadWriteLock` coordinates the game lifecycle
- A **ScheduledExecutorService** fires the game-end timer and triggers the next game
- Persistence uses one JSON file per user (`{id}_user.json`) and one per completed game (`game_N.json`), so saving one record never requires rewriting others

### Client
- Blocking-mode `SocketChannel` (NIO) for the TCP connection
- A daemon **UdpListener** thread for asynchronous server notifications — port is dynamically assigned by the OS (`local.udp.port = 0`) and communicated to the server at login
- Command-line interface

## Build

### Step by step with javac (required by spec)

All commands must be run from the **project root**. The modules must be compiled in order because `server` and `client` both depend on `net`.

```bash
# 1. Create output directories
mkdir -p build/net build/server build/client dist

# 2. Compile net module first (server and client depend on it)
javac --release 21 -cp lib/gson-2.10.1.jar \
      -d build/net \
      $(find net/src/main/java -name "*.java")

# 3. Compile server module
javac --release 21 -cp lib/gson-2.10.1.jar:build/net \
      -d build/server \
      $(find server/src/main/java -name "*.java")

# 4. Compile client module
javac --release 21 -cp lib/gson-2.10.1.jar:build/net \
      -d build/client \
      $(find client/src/main/java -name "*.java")

# 5. Package server fat JAR (includes net + Gson)
cd dist && mkdir -p _tmp && cd _tmp
jar xf ../../lib/gson-2.10.1.jar
cp -r ../../build/net/. .
cp -r ../../build/server/. .
printf "Manifest-Version: 1.0\nMain-Class: com.connectionsgame.server.ServerMain\n" > MANIFEST.MF
jar cfm ../server.jar MANIFEST.MF .
cd ../.. && rm -rf dist/_tmp

# 6. Package client fat JAR (includes net + Gson)
cd dist && mkdir -p _tmp && cd _tmp
jar xf ../../lib/gson-2.10.1.jar
cp -r ../../build/net/. .
cp -r ../../build/client/. .
printf "Manifest-Version: 1.0\nMain-Class: com.connectionsgame.client.ClientMain\n" > MANIFEST.MF
jar cfm ../client.jar MANIFEST.MF .
cd ../.. && rm -rf dist/_tmp
```

> **Note:** on Windows use `;` instead of `:` as the classpath separator.

### Using the build script (Linux/macOS shortcut)

The `compile.sh` script runs all the steps above automatically:

```bash
chmod +x compile.sh
./compile.sh
```

### Using Maven (alternative)

```bash
mvn package -DskipTests
```

JARs are located at:
- `server/target/server-1.0-jar-with-dependencies.jar`
- `client/target/client-1.0-jar-with-dependencies.jar`

## Running

Both commands must be run from the **project root** (where `server.properties` and `client.properties` are located).

```bash
# Start the server first
java -jar dist/server.jar

# In a separate terminal, start the client
java -jar dist/client.jar
```

## Configuration

### `server.properties`

| Parameter | Description | Default |
|---|---|---|
| `tcp.port` | TCP port the server listens on | `12345` |
| `udp.port` | UDP port for async notifications | `12346` |
| `game.duration.minutes` | Duration of each game | `5` |
| `thread.pool.core` | Core thread pool size | `CPU cores` |
| `thread.pool.max` | Maximum thread pool size | `CPU cores × 4` |
| `thread.keepalive.seconds` | Idle thread keep-alive time | `60` |
| `thread.queue.capacity` | Work queue capacity before backpressure | `200` |
| `puzzle.data.file` | Path to the puzzle data file | `./data/Connections_Data.json` |
| `users.dir` | Directory for user files | `./data/persistent/users` |
| `games.dir` | Directory for game result files | `./data/persistent/games` |
| `persistence.interval.seconds` | Periodic disk flush interval | `30` |

### `client.properties`

| Parameter | Description | Default |
|---|---|---|
| `server.host` | Server address | `localhost` |
| `server.tcp.port` | Server TCP port | `12345` |
| `local.udp.port` | Local UDP port (`0` = OS-assigned) | `0` |
| `read.timeout.ms` | TCP read timeout in milliseconds | `10000` |

## Client Commands

| Command | Description |
|---|---|
| `register <username> <password>` | Create a new account |
| `login <username> <password>` | Log in and display current game state |
| `logout` | Log out |
| `propose <w1> <w2> <w3> <w4>` | Submit a group proposal (4 individual words) |
| `gameinfo [gameId]` | State of the current game or a historical one |
| `gamestats [gameId]` | Aggregate statistics for a game |
| `leaderboard [top=K\|player=name]` | Full ranking, top-K, or single player position |
| `mystats` | Personal statistics |
| `help` | Show all available commands |
| `quit` | Exit the client |

### Example Session

```
> register alice password123
✓ Registration successful

> login alice password123
✓ Login successful

── Game #1 [inProgress] ──
Time remaining : 280s
Words to group :
SNOW    HAIL    BUCKS   JAZZ
SHIFT   TAB     RAIN    NETS
KAYAK   LEVEL   SLEET   MOM
RETURN  OPTION  RACECAR FOOT

> propose SNOW HAIL RAIN SLEET
✓ Correct! Theme: WET WEATHER
  Current score: 6

> mystats

── Your Statistics ──
Puzzles Completed : 3
Win Rate          : 66.7%
Loss Rate         : 0.0%
Current Streak    : 2
Max Streak        : 2
Perfect Puzzles   : 1
Mistake Histogram :
  0 mistakes: ## (2)
  1 mistakes: # (1)
  2 mistakes:  (0)
  3 mistakes:  (0)
  4 mistakes:  (0)
```

## Scoring

| Event | Points |
|---|---|
| 1 correct group | +6 |
| 2 correct groups | +12 |
| 3 correct groups (win) | +18 |
| Each wrong proposal | −4 |

## Persistence

The server saves data incrementally at multiple points:
- **On register** — user file created immediately
- **On game end** — each player's stats updated and flushed to their file; game result written to `game_N.json`
- **Periodic flush** — every 30 seconds (configurable), all cached users are written to disk as a safety net against sudden crashes

User files are named `{id}_user.json` where the numeric ID is stable across username changes. A small `index.json` maps usernames to IDs and is used for login lookups.
