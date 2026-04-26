package com.connectionsgame.server;

import com.connectionsgame.server.model.*;
import com.connectionsgame.server.storage.*;

import java.io.IOException;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.*;
import java.util.logging.Logger;

/**
 * Central coordinator for the game lifecycle.
 *
 * Responsibilities:
 *   1. Starting a new game (loading the next puzzle via PuzzleStorage).
 *   2. Validating and applying player proposals.
 *   3. Ending games (on timer expiry).
 *   4. Providing query methods used by all request handlers.
 *
 * Concurrency:
 *   - "currentGame" is protected by a ReadWriteLock:
 *       * Many request-handler threads hold the READ lock simultaneously.
 *       * Only the game-rotation task holds the WRITE lock when swapping games.
 *   - Per-player mutations (proposal submission) lock the PlayerGameState
 *     object itself, keeping the critical section as narrow as possible.
 *
 * Puzzle selection:
 *   Puzzles are identified by their gameId (0-based, 0..911 in the data file).
 *   The server's own game counter (nextGameId, 1-based) is mapped to a
 *   puzzle via:  puzzleGameId = (serverGameId - 1) % totalPuzzles
 *   This means game #1 uses puzzle 0, game #2 uses puzzle 1, etc.,
 *   and wraps around after all 912 puzzles have been played.
 */
public class GameManager {

    private static final Logger LOG = Logger.getLogger(GameManager.class.getName());

    private final PuzzleStorage  puzzleStorage;
    private final UserStorage    userStorage;
    private final GameStorage    gameStorage;
    private final UdpNotifier    udpNotifier;
    private final int            gameDurationMinutes;

    /** The single currently active game. Null before the first game starts. */
    private volatile GameSession currentGame;

    /**
     * ReadWriteLock protecting currentGame.
     * Multiple threads read it concurrently; the rotation task writes exclusively.
     */
    private final ReadWriteLock gameLock = new ReentrantReadWriteLock();

    /**
     * Single-thread scheduler that fires game-end events.
     * Daemon so it does not block JVM shutdown.
     */
    private final ScheduledExecutorService gameTimer =
            Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "game-timer");
                t.setDaemon(true);
                return t;
            });

    /**
     * Server-side game counter, 1-based (game #1, #2, ...).
     * Initialised from disk so the server resumes where it left off.
     */
    private int nextGameId;

    public GameManager(PuzzleStorage puzzleStorage, UserStorage   userStorage, GameStorage   gameStorage, UdpNotifier   udpNotifier, int gameDurationMinutes) {
        this.puzzleStorage = puzzleStorage;
        this.userStorage = userStorage;
        this.gameStorage = gameStorage;
        this.udpNotifier = udpNotifier;
        this.gameDurationMinutes = gameDurationMinutes;
        // Resume from last persisted game id (0 if first run)
        this.nextGameId = gameStorage.getLastGameId() + 1;
    }

    // ── Game lifecycle ────────────────────────────────────────────────────

    /**
     * Starts the next game.  Called once at server boot and after each game ends.
     *
     * The puzzle gameId wraps around using modulo so the server can run
     * indefinitely even with only 912 puzzles in the data file.
     */
    public void startNextGame() throws IOException {
        // Map server game counter (1-based) to puzzle gameId (0-based)
        int puzzleGameId = (nextGameId - 1) % puzzleStorage.getTotalPuzzles();
        Puzzle puzzle    = puzzleStorage.loadPuzzle(puzzleGameId);

        Instant now = Instant.now();
        Instant end = now.plusSeconds((long) gameDurationMinutes * 60);

        GameSession newGame = new GameSession(nextGameId, puzzle, now, end);
        int thisGameId = nextGameId;
        nextGameId++;

        // Write lock: swap the current game reference atomically
        gameLock.writeLock().lock();
        try {
            currentGame = newGame;
        } finally {
            gameLock.writeLock().unlock();
        }

        LOG.info("GameManager: started server game #" + thisGameId
                + " using puzzle gameId=" + puzzleGameId
                + " (" + puzzle.getGroups().get(0).getTheme() + " ...)"
                + ", ends in " + gameDurationMinutes + " min.");

        // Schedule automatic end; passing thisGameId prevents stale firings
        gameTimer.schedule(
                () -> endGame(thisGameId),
                gameDurationMinutes,
                TimeUnit.MINUTES
        );
    }

    /**
     * Ends the game with the given server gameId.
     *
     * Idempotent: if called for an already-replaced game (stale timer), does nothing.
     */
    public void endGame(int gameId) {
        GameSession game;
        gameLock.readLock().lock();
        try { game = currentGame; }
        finally { gameLock.readLock().unlock(); }

        if (game == null || game.getGameId() != gameId) {
            return; // stale timer firing, game already replaced
        }

        LOG.info("GameManager: ending game #" + gameId);

        // Mark all unfinished players as timed-out
        game.getAllPlayerStates().forEach((username, state) -> {
            synchronized (state) { state.markTimedOut(); }
        });

        // Persist the result and update user stats
        try {
            GameResult result = buildGameResult(game);
            gameStorage.saveGame(result);
            updateUserStats(game, result);
        } catch (IOException e) {
            LOG.severe("GameManager: failed to persist game #" + gameId + ": " + e.getMessage());
        }

        // Broadcast game-over to all connected clients via UDP
        udpNotifier.broadcastGameOver(game);

        // Start next game immediately
        try {
            startNextGame();
        } catch (IOException e) {
            LOG.severe("GameManager: failed to start next game: " + e.getMessage());
        }
    }

    // ── Proposal handling ─────────────────────────────────────────────────

    /**
     * Validates and applies a player's group proposal.
     *
     * Validation rules (from the spec):
     *   - Must contain exactly 4 words.
     *   - All words must be part of the current puzzle.
     *   - No word may already be correctly grouped by this player.
     *   Violations are MALFORMED errors: they do NOT count as mistakes.
     *
     * @return a ProposalResult describing the outcome.
     */
    public ProposalResult submitProposal(String username, List<String> words) {
        gameLock.readLock().lock();
        GameSession game;
        try { game = currentGame; }
        finally { gameLock.readLock().unlock(); }

        if (game == null)      return ProposalResult.error("No active game");
        if (game.isExpired())  return ProposalResult.error("The game has already ended");

        PlayerGameState state = game.getOrCreatePlayerState(username);

        synchronized (state) {
            if (state.isFinished()) {
                return ProposalResult.error("You have already finished this game");
            }

            // ── Structural validation (malformed = no mistake) ─────────────
            if (words == null || words.size() != 4) {
                return ProposalResult.malformed("A proposal must contain exactly 4 words");
            }

            Puzzle       puzzle    = game.getPuzzle();
            List<String> allWords  = puzzle.getAllWords();
            List<String> remaining = state.getRemainingWords(puzzle);

            for (String w : words) {
                String upper = w.toUpperCase();
                if (!allWords.contains(upper)) {
                    return ProposalResult.malformed("'" + w + "' is not part of this puzzle");
                }
                if (!remaining.contains(upper)) {
                    return ProposalResult.malformed("'" + w + "' has already been correctly grouped");
                }
            }

            List<String> normalised = words.stream().map(String::toUpperCase).toList();

            // ── Game logic ─────────────────────────────────────────────────
            PuzzleGroup match = puzzle.findMatchingGroup(normalised);

            if (match != null) {
                boolean won = state.recordCorrectProposal(match);
                return ProposalResult.correct(match, state.getCurrentScore(), won);
            } else {
                boolean lost = state.recordWrongProposal();
                return ProposalResult.wrong(state.getMistakes(), state.getCurrentScore(), lost);
            }
        }
    }

    // ── Query methods ─────────────────────────────────────────────────────

    public GameSession getCurrentGame() {
        return currentGame;
    }

    public GameResult getHistoricalGame(int gameId) {
        return gameStorage.loadGame(gameId);
    }

    // ── Internal helpers ──────────────────────────────────────────────────

    private GameResult buildGameResult(GameSession game) {
        // Build solution map: theme -> words
        Map<String, List<String>> solution = new LinkedHashMap<>();
        for (PuzzleGroup g : game.getPuzzle().getGroups()) {
            solution.put(g.getTheme(), g.getWords());
        }

        Map<String, GameResult.PlayerSummary> summaries = new LinkedHashMap<>();
        game.getAllPlayerStates().forEach((username, state) ->
                summaries.put(username, new GameResult.PlayerSummary(
                        state.getCorrectGroups().size(),
                        state.getMistakes(),
                        state.getCurrentScore(),
                        state.isWon(),
                        state.isFinished()
                ))
        );

        return new GameResult(
                game.getGameId(),
                game.getStartTime().getEpochSecond(),
                Instant.now().getEpochSecond(),
                solution,
                summaries
        );
    }

    private void updateUserStats(GameSession game, GameResult result) {
        result.getPlayerSummaries().forEach((username, summary) -> {
            User user = userStorage.getByUsername(username);
            if (user == null) return;
            synchronized (user) {
                user.recordGameResult(
                        summary.getScore(),
                        summary.getMistakes(),
                        summary.isWon(),
                        summary.isCompleted()
                );
                try { userStorage.saveUser(user); }
                catch (IOException e) {
                    LOG.warning("Could not save user stats for " + username + ": " + e.getMessage());
                }
            }
        });
    }

    // ── Inner result type ─────────────────────────────────────────────────

    /** The outcome of a single proposal submission. */
    public static class ProposalResult {

        public enum Kind { CORRECT, WRONG, MALFORMED, ERROR }

        private final Kind        kind;
        private final PuzzleGroup matchedGroup; // non-null only for CORRECT
        private final int         currentScore;
        private final int         mistakeCount;
        private final boolean     gameOver;
        private final String      message;

        private ProposalResult(Kind kind, PuzzleGroup matchedGroup, int currentScore,
                               int mistakeCount, boolean gameOver, String message) {
            this.kind         = kind;
            this.matchedGroup = matchedGroup;
            this.currentScore = currentScore;
            this.mistakeCount = mistakeCount;
            this.gameOver     = gameOver;
            this.message      = message;
        }

        public static ProposalResult correct(PuzzleGroup group, int score, boolean won) {
            return new ProposalResult(Kind.CORRECT, group, score, 0, won, null);
        }
        public static ProposalResult wrong(int mistakes, int score, boolean lost) {
            return new ProposalResult(Kind.WRONG, null, score, mistakes, lost, null);
        }
        public static ProposalResult malformed(String msg) {
            return new ProposalResult(Kind.MALFORMED, null, 0, 0, false, msg);
        }
        public static ProposalResult error(String msg) {
            return new ProposalResult(Kind.ERROR, null, 0, 0, false, msg);
        }

        public Kind getKind(){ return kind; }
        public PuzzleGroup getMatchedGroup() { return matchedGroup; }
        public int getCurrentScore() { return currentScore; }
        public int getMistakeCount() { return mistakeCount; }
        public boolean isGameOver() { return gameOver; }
        public String getMessage() { return message; }
    }
}
