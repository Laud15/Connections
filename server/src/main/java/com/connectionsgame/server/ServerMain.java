package com.connectionsgame.server;

import com.connectionsgame.server.config.ServerConfig;
import com.connectionsgame.server.storage.*;

import java.util.concurrent.*;
import java.util.logging.*;

/**
 * Entry point for the Connections game server.
 *
 * Start-up sequence:
 *   1. Load config from server.properties
 *   2. Init storage (split master puzzles file if needed, load users)
 *   3. Start UDP notifier
 *   4. Start GameManager (starts first game)
 *   5. Schedule periodic persistence task
 *   6. Start NIO server (blocks until shutdown)
 *
 * Classes containing main must have "Main" in their name (per project spec).
 */
public class ServerMain {

    private static final Logger LOG = Logger.getLogger(ServerMain.class.getName());

    public static void main(String[] args) throws Exception {
        // ── 1. Configuration ──────────────────────────────────────────────
        String configPath = "server.properties";
        ServerConfig config = new ServerConfig(configPath);
        LOG.info("ServerMain: loaded config from " + configPath);

        // ── 2. Storage layer ──────────────────────────────────────────────
        PuzzleStorage puzzleStorage = new PuzzleStorage(config.getPuzzleDataFile());
        UserStorage   userStorage   = new UserStorage(config.getUserDir());
        GameStorage   gameStorage   = new GameStorage(config.getGamesDir());

        // ── 3. UDP notifier ───────────────────────────────────────────────
        UdpNotifier udpNotifier = new UdpNotifier(config.getUdpPort());

        // ── 4. Game manager + first game ──────────────────────────────────
        GameManager gameManager = new GameManager(
                puzzleStorage,
                userStorage,
                gameStorage,
                udpNotifier,
                config.getGameDurationMinutes());
        gameManager.startNextGame();

        // ── 5. Periodic persistence ───────────────────────────────────────
        // Flush all in-memory user data to disk every N seconds as a safety net.
        // Individual users are also saved immediately after each game ends,
        // so this is only needed to cover the case of a sudden server crash
        // mid-game (e.g. power loss) that would otherwise lose recent score changes.
        ScheduledExecutorService persistenceScheduler =
                Executors.newSingleThreadScheduledExecutor(r -> {
                    Thread t = new Thread(r, "persistence-flush");
                    t.setDaemon(true);
                    return t;
                });

        persistenceScheduler.scheduleAtFixedRate(
                () -> {
                    LOG.fine("Periodic persistence flush...");
                    userStorage.saveAll();
                },
                config.getPersistenceIntervalSeconds(),
                config.getPersistenceIntervalSeconds(),
                TimeUnit.SECONDS
        );

        // ── 6. NIO server (runs until the JVM exits) ──────────────────────
        NioServer nioServer = new NioServer(
                config.getTcpPort(),
                config.getThreadPoolCore(),
                config.getThreadPoolMax(),
                config.getThreadKeepAliveSeconds(),
                config.getThreadQueueCapacity(),
                gameManager,
                userStorage
        );

        // Register shutdown hook so Ctrl+C triggers a clean flush
        Runtime.getRuntime().addShutdownHook(
                new Thread(() -> {
            LOG.info("Shutdown hook: flushing user data...");
            userStorage.saveAll();
            udpNotifier.close();
        }, "shutdown-hook"));

        // Run the NIO event loop on the main thread
        nioServer.run();
    }
}
