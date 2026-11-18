# Connections — Client–Server Word Game

University networking project inspired by the Connections word-grouping puzzle.
Multiple command-line clients share a timed puzzle, with individual progress,
accounts, scores, statistics and a leaderboard.

Each puzzle contains 16 words in four groups. This implementation awards a win
after **three correct groups** and a loss after four mistakes. Each correct group
adds 6 points; each wrong proposal subtracts 4. The server starts the next puzzle
when the shared timer expires.

## Technologies and design

- Java 21 target; Gson for JSON requests, responses and storage. The build
  scripts use the bundled Gson 2.10.1 JAR; Maven declares Gson 2.11.0.
- A Java NIO `Selector` handles server TCP connections. A bounded
  `ThreadPoolExecutor` processes requests and returns responses through a
  `ConcurrentLinkedQueue`; the selector thread performs socket writes.
- The client uses a blocking `SocketChannel` with newline-delimited JSON.
  A separate UDP listener receives game-end notifications.
- An `AtomicReference` publishes the current game; synchronization on each
  player's state protects proposal handling. A scheduled executor rotates games.
- Puzzles are streamed from a 912-entry JSON dataset. User records and completed
  games are saved as separate JSON files, with a username-to-ID index.

## Prerequisites and build

Use a complete **JDK 21 or newer**, with `java`, `javac` and `jar` on `PATH`, and
Bash for the following build command. Run all commands from the repository root.
The bundled Gson JAR is sufficient for this build; Maven is optional.

```sh
bash build.sh
```

The script recreates `build/` and `dist/`, producing `dist/server.jar` and
`dist/client.jar`. Gson must also be present on the runtime classpath.

## Minimal example

Start the server in one terminal:

```sh
java -cp "dist/server.jar:lib/gson-2.10.1.jar" com.connectionsgame.server.ServerMain
```

Start the client in another terminal:

```sh
java -cp "dist/client.jar:lib/gson-2.10.1.jar" com.connectionsgame.client.ClientMain
```

The existing `.sh` launchers are swapped: `run-server.sh` starts the client and
`run-client.sh` starts the server. Use the explicit commands above. On Windows,
the classpath separator is `;` instead of `:`.

For a fresh data directory, the first puzzle accepts the proposal below. Later
games use different words; use the words displayed by the client.

```text
register readme_demo demo-password
login readme_demo demo-password
gameinfo
propose SNOW HAIL RAIN SLEET
logout
quit
```

Use underscores for spaces within a proposed phrase. `help` lists the remaining
commands, including `mystats`, `gamestats`, `leaderboard` and `updatecredentials`.
Stop the server with Ctrl+C.

## Configuration and files

The supplied settings use TCP port **12345**, server UDP port **12346**,
three-minute games and a 30-second user-data flush interval. The client connects
to `localhost` and requests an OS-assigned UDP port. Paths are relative to the
repository root; persistence directories are created automatically.

| Path | Purpose |
| --- | --- |
| `net/` | Shared request/response classes and JSON deserializers |
| `server/` | NIO server, game logic and storage |
| `client/` | Command-line interface, TCP connection and UDP listener |
| `data/Connections_Data.json` | Puzzle dataset |
| `data/persistent/` | Generated user records, index and completed-game results |
| `server.properties`, `client.properties` | Network, timing and storage settings |
| `lib/`, `build.sh`, `pom.xml` | Bundled dependency and build entry points |

## Verification

The two explicit launch commands and a short local CLI session were verified in
a temporary copy on Linux with OpenJDK 25: registration, login, a correct
proposal, game information, personal statistics, leaderboard, logout and quit.

Sources were compiled using the installed compiler module with
`-source 21 -target 21` against Java 25 APIs. **`bash build.sh` remains unverified
with a complete JDK:** its `--release 21` step was attempted but unsupported by
the available runtime image. Java 21 runtime compatibility, Maven/Windows builds,
timer expiry, UDP delivery and concurrent load remain unverified.

## Limitations

- Passwords are transmitted over unencrypted TCP and stored in plain text.
  Password hashing and TLS are not implemented.
- `read.timeout.ms` is currently unused. When the worker pool and queue are full,
  rejected requests are logged without a response, so a client can remain blocked.
- Network buffers have fixed capacities; oversized messages and pending writes
  are not handled robustly.
- Active-game progress is held in memory and is lost on restart. JSON writes
  are not transactional.
