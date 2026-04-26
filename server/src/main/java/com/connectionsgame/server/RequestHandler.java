package com.connectionsgame.server;

import com.connectionsgame.RequestDeserializer;
import com.connectionsgame.abstract_class.Request;
import com.connectionsgame.requests.*;
import com.connectionsgame.responses.*;
import com.connectionsgame.server.model.*;
import com.connectionsgame.server.storage.UserStorage;
import com.google.gson.Gson;

import java.io.IOException;
import java.util.*;
import java.util.logging.Logger;

/**
 * Processes one client request and produces a JSON response string.
 *
 * Lifecycle:
 *   1. NioServer receives a complete '\n'-terminated JSON message.
 *   2. NioServer submits a new RequestHandler(rawJson, session, ...) to the thread pool.
 *   3. run() parses and dispatches the request, builds a typed Response object,
 *      serializes it to JSON, and hands the result back via WriteCallback.
 *   4. The Selector thread (NioServer) writes the JSON string to the SocketChannel.
 *
 * This class holds no shared mutable state — all inputs are via constructor.
 * Thread safety is therefore guaranteed without any synchronization in this class.
 */
public class RequestHandler implements Runnable {

    private static final Logger LOG  = Logger.getLogger(RequestHandler.class.getName());
    private static final Gson   GSON = new Gson();

    private final String        rawJson;
    private final ClientSession session;
    private final GameManager   gameManager;
    private final UserStorage   userStorage;
    private final WriteCallback callback;

    /**
     * Called by a worker thread when the response is ready.
     * Hands the JSON string back to the Selector thread for writing.
     */
    @FunctionalInterface
    public interface WriteCallback {
        void onResponse(ClientSession session, String responseJson);
    }

    public RequestHandler(String rawJson, ClientSession session, GameManager gameManager, UserStorage userStorage, WriteCallback callback) {
        this.rawJson     = rawJson;
        this.session     = session;
        this.gameManager = gameManager;
        this.userStorage = userStorage;
        this.callback    = callback;
    }

    @Override
    public void run() {
        String response;
        try {
            Request request = RequestDeserializer.parse(rawJson);
            response = dispatch(request);
        } catch (IllegalArgumentException e) {
            response = error(400, e.getMessage());
        } catch (Exception e) {
            LOG.severe("Unhandled error processing [" + rawJson + "]: " + e.getMessage());
            response = error(500, "Internal server error");
        }
        callback.onResponse(session, response);
    }

    // ── Dispatcher ────────────────────────────────────────────────────────

    private String dispatch(Request req) throws IOException {
        return switch (req.getOperation()) {
            case "register"           -> handleRegister((RegisterRequest) req);
            case "updateCredentials"  -> handleUpdateCredentials((UpdateCredentialsRequest) req);
            case "login"              -> handleLogin((LoginRequest) req);
            case "logout"             -> handleLogout();
            case "submitProposal"     -> handleSubmitProposal((SubmitProposalRequest) req);
            case "requestGameInfo"    -> handleRequestGameInfo((GameInfoRequest) req);
            case "requestGameStats"   -> handleRequestGameStats((GameStatsRequest) req);
            case "requestLeaderboard" -> handleRequestLeaderboard((LeaderBoardRequest) req);
            case "requestPlayerStats" -> handleRequestPlayerStats();
            default                   -> error(400, "Unknown operation: " + req.getOperation());
        };
    }

    // ── Handlers ──────────────────────────────────────────────────────────

    private String handleRegister(RegisterRequest req) throws IOException {
        if (blank(req.getName()))
            return error(400, "Username cannot be empty");
        if (blank(req.getPsw()))
            return error(400, "Password cannot be empty");

        boolean ok = userStorage.register(req.getName(), req.getPsw());
        if (!ok)
            return error(409, "Username '" + req.getName() + "' is already taken");

        return json(new RegisterResponse());
    }

    private String handleUpdateCredentials(UpdateCredentialsRequest req) throws IOException {
        if (blank(req.getOldName()) || blank(req.getOldPsw()))
            return error(400, "oldName and oldPsw are required");
        if (req.getNewName() == null && req.getNewPsw() == null)
            return error(400, "At least one of newName or newPsw must be provided");

        String errMsg = userStorage.updateCredentials(
                req.getOldName(), req.getOldPsw(), req.getNewName(), req.getNewPsw());
        if (errMsg != null)
            return error(401, errMsg);

        return json(new UpdateCredentialsResponse("Credentials updated successfully"));
    }

    private String handleLogin(LoginRequest req) {
        if (session.isLoggedIn())
            return error(403, "Already logged in as " + session.getLoggedInUserName());
        if (blank(req.getUsername()) || blank(req.getPwd()))
            return error(400, "username and psw are required");

        User user = userStorage.authenticate(req.getUsername(), req.getPwd());
        if (user == null)
            return error(401, "Wrong username or password");

        session.login(req.getUsername(), req.getUdpPort());

        GameSession game = gameManager.getCurrentGame();
        LoginResponse resp;

        if (game != null) {
            PlayerGameState state = game.getOrCreatePlayerState(req.getUsername());
            resp = new LoginResponse(
                    "Login successful",
                    game.getGameId(),
                    game.getRemainingSeconds(),
                    shuffled(state.getRemainingWords(game.getPuzzle())),
                    themeNames(state.getCorrectGroups()),
                    state.getMistakes(),
                    state.getCurrentScore()
            );
        } else {
            resp = new LoginResponse("Login successful");
        }

        return json(resp);
    }

    private String handleLogout() {
        if (!session.isLoggedIn())
            return error(401, "Not logged in");

        session.logout();
        return json(new LogoutResponse("Logged out successfully"));
    }

    private String handleSubmitProposal(SubmitProposalRequest req) {
        if (!session.isLoggedIn())
            return error(401, "Not logged in");

        GameManager.ProposalResult result =
                gameManager.submitProposal(session.getLoggedInUserName(), req.getWords());

        return switch (result.getKind()) {
            case CORRECT -> {
                PuzzleGroup g = result.getMatchedGroup();
                yield json(new SubmitProposalResponse(
                        g.getTheme(), g.getWords(),
                        result.getCurrentScore(),
                        result.isGameOver(),
                        result.isGameOver() ? true : null));
            }
            case WRONG -> json(new SubmitProposalResponse(
                    result.getMistakeCount(),
                    result.getCurrentScore(),
                    result.isGameOver(),
                    result.isGameOver() ? false : null));
            case MALFORMED -> error(400, result.getMessage());
            case ERROR     -> error(403, result.getMessage());
        };
    }

    private String handleRequestGameInfo(GameInfoRequest req) {
        if (!session.isLoggedIn())
            return error(401, "Not logged in");

        int         gameId  = req.getId();
        GameSession current = gameManager.getCurrentGame();

        // -1 or matching current gameId → return live state
        if (gameId == -1 || (current != null && gameId == current.getGameId())) {
            if (current == null)
                return error(404, "No active game");

            PlayerGameState state = current.getPlayerState(session.getLoggedInUserName());
            if (state == null) {
                // Player hasn't joined yet — create a fresh state and return empty info
                state = current.getOrCreatePlayerState(session.getLoggedInUserName());
            }
            return json(new GameInfoResponse(
                    current.getGameId(),
                    current.getRemainingSeconds(),
                    themeNames(state.getCorrectGroups()),
                    state.getRemainingWords(current.getPuzzle()),
                    state.getMistakes(),
                    state.getCurrentScore()
            ));
        }

        // Historical game
        GameResult result = gameManager.getHistoricalGame(gameId);
        if (result == null)
            return error(404, "Game #" + gameId + " not found");

        GameResult.PlayerSummary summary =
                result.getPlayerSummaries().get(session.getLoggedInUserName());

        int correct = summary != null ? summary.getCorrectGroups() : 0;
        int mistakes = summary != null ? summary.getMistakes()     : 0;
        int score    = summary != null ? summary.getScore()        : 0;

        return json(new GameInfoResponse(
                result.getGameId(),
                result.getSolution(),
                correct, mistakes, score
        ));
    }

    private String handleRequestGameStats(GameStatsRequest req) {
        if (!session.isLoggedIn())
            return error(401, "Not logged in");

        int         gameId  = req.getId();
        GameSession current = gameManager.getCurrentGame();

        if (gameId == -1 || (current != null && gameId == current.getGameId())) {
            if (current == null) {
                return error(404, "No active game");
            }

            return json(new GameStatsResponse(
                    current.getGameId(),
                    current.getRemainingSeconds(),
                    (int) current.getPlayersStillPlaying(),
                    (int) current.getPlayersFinished(),
                    (int) current.getPlayersWon()
            ));
        }

        GameResult result = gameManager.getHistoricalGame(gameId);
        if (result == null)
            return error(404, "Game #" + gameId + " not found");

        return json(new GameStatsResponse(
                result.getGameId(),
                result.getTotalPlayers(),
                (int) result.getCompletedCount(),
                (int) result.getWinCount(),
                result.getAverageScore()
        ));
    }

    private String handleRequestLeaderboard(LeaderBoardRequest req) {
        if (!session.isLoggedIn())
            return error(401, "Not logged in");

        List<User> board = userStorage.getLeaderboard();

        // Mode 1: single-player ranking
        if (req.getPlayerName() != null) {
            String target = req.getPlayerName();
            for (int i = 0; i < board.size(); i++) {
                if (board.get(i).getUsername().equals(target)) {
                    return json(new LeaderBoardResponse(
                            target,
                            i + 1,
                            board.get(i).getTotalScore()
                    ));
                }
            }
            return error(404, "Player '" + target + "' not found");
        }

        // Mode 2: full list or top-K
        int limit = (req.getTopPlayers() > 0)
                ? Math.min(req.getTopPlayers(), board.size())
                : board.size();

        List<LeaderBoardResponse.LeaderboardEntry> entries = new ArrayList<>();
        for (int i = 0; i < limit; i++) {
            User u = board.get(i);
            entries.add(new LeaderBoardResponse.LeaderboardEntry(i + 1, u.getUsername(), u.getTotalScore()));
        }
        return json(new LeaderBoardResponse(entries));
    }

    private String handleRequestPlayerStats() {
        if (!session.isLoggedIn())
            return error(401, "Not logged in");

        User user = userStorage.getByUsername(session.getLoggedInUserName());
        if (user == null)
            return error(404, "User not found");

        return json(new PlayerStatsResponse(
                user.getPuzzlesCompleted(),
                user.getWinRate(),
                user.getLossRate(),
                user.getCurrentStreak(),
                user.getMaxStreak(),
                user.getPerfectPuzzles(),
                user.getMistakeHistogram()
        ));
    }

    // ── Response helpers ──────────────────────────────────────────────────

    /**
     * Serialize any object to a '\n'-terminated JSON string.
     * The newline is the message delimiter understood by ServerConnection on the client.
     */
    private String json(Object obj) {
        return GSON.toJson(obj) + "\n";
    }

    /** Build an error envelope. */
    private String error(int code, String message) {
        return json(Map.of(
                "status",       "error",
                "errorCode",    code,
                "errorMessage", message
        ));
    }

    // ── Misc helpers ──────────────────────────────────────────────────────

    private boolean blank(String s) {
        return s == null || s.isBlank();
    }

    private List<String> themeNames(List<PuzzleGroup> groups) {
        return groups.stream().map(PuzzleGroup::getTheme).toList();
    }

    private List<String> shuffled(List<String> words) {
        List<String> copy = new ArrayList<>(words);
        Collections.shuffle(copy);
        return copy;
    }
}
