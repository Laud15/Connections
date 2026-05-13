package com.connectionsgame.client;

import com.connectionsgame.ResponseDeserializer;
import com.connectionsgame.abstract_class.Response;
import com.connectionsgame.client.config.ClientConfig;
import com.connectionsgame.requests.*;
import com.connectionsgame.responses.*;

import java.util.*;
import java.util.logging.Logger;

/**
 * Command-line interface for the Connections game client.
 *
 * Uses ResponseDeserializer to turn every server response into a typed
 * Response subclass, eliminating raw JsonObject field access.
 *
 * Available commands:
 *   register <username> <password>
 *   updatecredentials <oldName> <oldPsw> [newname=<n>] [newpsw=<p>]
 *   login <username> <password>
 *   logout
 *   propose <word1> <word2> <word3> <word4>
 *   gameinfo [gameId]
 *   gamestats [gameId]
 *   leaderboard [top=K | player=<name>]
 *   mystats
 *   help
 *   quit
 */
public class ClientMain {

    private static final Logger LOG = Logger.getLogger(ClientMain.class.getName());

    public static void main(String[] args) {
        // ── Config ────────────────────────────────────────────────────────
        ClientConfig config;
        try {
            config = new ClientConfig("client.properties");
        } catch (Exception e) {
            System.err.println("ERROR: Could not read client.properties — " + e.getMessage());
            System.exit(1);
            return;
        }

        // ── UDP listener (async game-over notifications) ───────────────────
        UdpListener udpListener = new UdpListener(config.getLocalUdpPort());
        udpListener.start();

        // ── TCP connection to server ──────────────────────────────────────
        ServerConnection conn;
        try {
            conn = new ServerConnection(config.getServerHost(), config.getServerTcpPort());
        } catch (Exception e) {
            System.err.println("ERROR: Cannot connect to server — " + e.getMessage());
            System.exit(1);
            return;
        }

        System.out.println("=== Connections Game Client ===");
        System.out.println("Connected to " + config.getServerHost()
                + ":" + config.getServerTcpPort());
        System.out.println("Type 'help' to see available commands.");

        Scanner scanner = new Scanner(System.in);
        boolean running = true;

        while (running) {
            System.out.print("> ");
            String line = scanner.nextLine().trim();
            if (line.isEmpty()) continue;

            String[] parts = line.split("\\s+");
            String   cmd   = parts[0].toLowerCase();

            try {
                switch (cmd) {
                    case "help"              -> printHelp();
                    case "quit", "exit"      -> running = false;
                    case "register"          -> doRegister(conn, parts);
                    case "updatecredentials" -> doUpdateCredentials(conn, parts);
                    case "login"             -> doLogin(conn, parts, config.getLocalUdpPort());
                    case "logout"            -> doLogout(conn);
                    case "propose"           -> doPropose(conn, parts);
                    case "gameinfo"          -> doGameInfo(conn, parts);
                    case "gamestats"         -> doGameStats(conn, parts);
                    case "leaderboard"       -> doLeaderboard(conn, parts);
                    case "mystats"           -> doMyStats(conn);
                    default -> System.out.println("Unknown command '" + cmd + "'. Type 'help'.");
                }
            } catch (Exception e) {
                System.err.println("Error: " + e.getMessage());
                LOG.warning("Command '" + cmd + "' failed: " + e.getMessage());
            }
        }

        System.out.println("Goodbye!");
        conn.close();
        udpListener.stopListening();
        scanner.close();
    }

    // ── Command implementations ───────────────────────────────────────────

    private static void doRegister(ServerConnection conn, String[] parts) throws Exception {
        if (parts.length < 3) {
            System.out.println("Usage: register <username> <password>");
            return;
        }
        conn.send(new RegisterRequest(parts[1], parts[2]));
        Response resp = deserialize(conn);
        if (resp instanceof RegisterResponse r) {
            System.out.println("✓ " + r.getMessage());
        }
    }

    private static void doUpdateCredentials(ServerConnection conn, String[] parts) throws Exception {
        if (parts.length < 3) {
            System.out.println("Usage: updatecredentials <oldName> <oldPsw> [newname=<n>] [newpsw=<p>]");
            return;
        }
        String oldName = parts[1], oldPsw = parts[2];
        String newName = null, newPsw = null;
        for (int i = 3; i < parts.length; i++) {
            if (parts[i].startsWith("newname=")) newName = parts[i].substring(8);
            if (parts[i].startsWith("newpsw="))  newPsw  = parts[i].substring(7);
        }
        conn.send(new UpdateCredentialsRequest(oldName, oldPsw, newName, newPsw));
        Response resp = deserialize(conn);
        if (resp instanceof UpdateCredentialsResponse r) {
            System.out.println("✓ " + r.getMessage());
        }
    }

    private static void doLogin(ServerConnection conn, String[] parts, int udpPort) throws Exception {
        if (parts.length < 3) {
            System.out.println("Usage: login <username> <password>");
            return;
        }
        conn.send(new LoginRequest(parts[1], parts[2], udpPort));
        Response resp = deserialize(conn);
        if (resp instanceof LoginResponse r) {
            System.out.println("✓ " + r.getMessage());
            if (r.getGameId() != null) {
                System.out.println("\n── Game #" + r.getGameId() + " ──");
                System.out.println("Time remaining : " + r.getRemainingSeconds() + "s");
                System.out.println("Mistakes so far: " + r.getMistakes());
                System.out.println("Score so far   : " + r.getCurrentScore());
                if (r.getCorrectGroups() != null && !r.getCorrectGroups().isEmpty()) {
                    System.out.println("Groups found   : " + r.getCorrectGroups());
                }
                if (r.getWords() != null) {
                    System.out.println("Words to group :");
                    printWordGrid(r.getWords());
                }
            } else {
                System.out.println("(No active game at the moment)");
            }
        }
    }

    private static void doLogout(ServerConnection conn) throws Exception {
        conn.send(new LogoutRequest());
        Response resp = deserialize(conn);
        if (resp instanceof LogoutResponse r) {
            System.out.println("✓ " + r.getMessage());
        }
    }

    private static void doPropose(ServerConnection conn, String[] parts) throws Exception {
        if (parts.length < 5) {
            System.out.println("Usage: propose <w1> <w2> <w3> <w4>");
            return;
        }
        conn.send(new SubmitProposalRequest(List.of(parts[1], parts[2], parts[3], parts[4])));
        Response resp = deserialize(conn);
        if (resp instanceof SubmitProposalResponse r) {
            if (r.isCorrect()) {
                System.out.println("✓ Correct! Theme: " + r.getTheme());
                System.out.println("  Words: " + r.getWords());
            } else {
                System.out.println("✗ Wrong! Total mistakes: " + r.getMistakes());
            }
            System.out.println("  Current score: " + r.getCurrentScore());
            if (r.isGameOver()) {
                System.out.println(Boolean.TRUE.equals(r.getWon())
                        ? "\n🎉 You WON!" : "\n💀 You LOST.");
            }
        }
    }

    private static void doGameInfo(ServerConnection conn, String[] parts) throws Exception {
        int gameId = parts.length >= 2 ? Integer.parseInt(parts[1]) : -1;
        conn.send(new GameInfoRequest(gameId));
        Response resp = deserialize(conn);
        if (resp instanceof GameInfoResponse r) {
            System.out.println("\n── Game #" + r.getGameId()
                    + " [" + r.getGameStatus() + "] ──");
            if ("inProgress".equals(r.getGameStatus())) {
                System.out.println("Time remaining : " + r.getRemainingSeconds() + "s");
                System.out.println("Mistakes       : " + r.getMistakes());
                System.out.println("Score          : " + r.getCurrentScore());
                if (r.getCorrectGroups() != null && !r.getCorrectGroups().isEmpty()) {
                    System.out.println("Groups found   : " + r.getCorrectGroups());
                }
                if (r.getRemainingWords() != null) {
                    System.out.println("Remaining words:");
                    printWordGrid(r.getRemainingWords());
                }
            } else {
                System.out.println("Correct groups : " + r.getCorrectCount());
                System.out.println("Mistakes       : " + r.getMistakes());
                System.out.println("Final score    : " + r.getFinalScore());
                if (r.getSolution() != null) {
                    System.out.println("\n── Solution ──");
                    r.getSolution().forEach((theme, words) ->
                            System.out.println("  " + theme + ": " + words));
                }
            }
        }
    }

    private static void doGameStats(ServerConnection conn, String[] parts) throws Exception {
        int gameId = parts.length >= 2 ? Integer.parseInt(parts[1]) : -1;
        conn.send(new GameStatsRequest(gameId));
        Response resp = deserialize(conn);
        if (resp instanceof GameStatsResponse r) {
            System.out.println("\n── Game #" + r.getGameId()
                    + " Stats [" + r.getGameStatus() + "] ──");
            if ("inProgress".equals(r.getGameStatus())) {
                System.out.println("Time remaining   : " + r.getRemainingSeconds() + "s");
                System.out.println("Still playing    : " + r.getPlayersPlaying());
                System.out.println("Finished         : " + r.getPlayersFinished());
                System.out.println("Won              : " + r.getPlayersWon());
            } else {
                System.out.println("Total players    : " + r.getTotalPlayers());
                System.out.println("Completed        : " + r.getCompleted());
                System.out.println("Won              : " + r.getWon());
                System.out.println("Average score    : " + r.getAverageScore());
            }
        }
    }

    private static void doLeaderboard(ServerConnection conn, String[] parts) throws Exception {
        String playerName = null;
        int topK = 0;
        for (int i = 1; i < parts.length; i++) {
            if (parts[i].startsWith("top="))    topK       = Integer.parseInt(parts[i].substring(4));
            if (parts[i].startsWith("player=")) playerName = parts[i].substring(7);
        }
        conn.send(new LeaderBoardRequest(playerName, topK));
        Response resp = deserialize(conn);
        if (resp instanceof LeaderBoardResponse r) {
            if (r.getLeaderboard() != null) {
                System.out.println("\n── Leaderboard ──");
                r.getLeaderboard().forEach(e ->
                        System.out.printf("#%-3d %-20s %d pts%n",
                                e.getRank(), e.getUsername(), e.getScore()));
            } else {
                System.out.printf("#%d  %s — %d pts%n",
                        r.getRank(), r.getUsername(), r.getScore());
            }
        }
    }

    private static void doMyStats(ServerConnection conn) throws Exception {
        conn.send(new PlayerStatsRequest());
        Response resp = deserialize(conn);
        if (resp instanceof PlayerStatsResponse r) {
            System.out.println("\n── Your Statistics ──");
            System.out.println("Puzzles Completed : " + r.getPuzzlesCompleted());
            System.out.printf( "Win Rate          : %.1f%%%n", r.getWinRate());
            System.out.printf( "Loss Rate         : %.1f%%%n", r.getLossRate());
            System.out.println("Current Streak    : " + r.getCurrentStreak());
            System.out.println("Max Streak        : " + r.getMaxStreak());
            System.out.println("Perfect Puzzles   : " + r.getPerfectPuzzles());
            System.out.println("\nMistake Histogram :");
            int[] hist = r.getMistakeHistogram();
            for (int i = 0; i < hist.length; i++) {
                String bar = "#".repeat(Math.max(0, hist[i]));
                System.out.printf("  %d mistakes: %s (%d games)%n", i, bar, hist[i]);
            }
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    /**
     * Receive one response from the server and deserialize it.
     * If it's an error, prints the error message and returns the ErrorResponse.
     */
    private static Response deserialize(ServerConnection conn) throws Exception {
        String raw = conn.receive();
        Response resp = ResponseDeserializer.parse(raw);
        if (resp instanceof ErrorResponse e) {
            System.out.println("SERVER ERROR " + e.getErrorCode()
                    + ": " + e.getErrorMessage());
        }
        return resp;
    }

    /** Prints up to 16 words in a 4-column grid. */
    private static void printWordGrid(List<String> words) {
        int col = 0;
        for (String w : words) {
            System.out.printf("%-18s", w);
            if (++col == 4) { System.out.println(); col = 0; }
        }
        if (col != 0) System.out.println();
    }

    private static void printHelp() {
        System.out.println("""
                Commands:
                  register <username> <password>
                  updatecredentials <oldName> <oldPsw> [newname=<n>] [newpsw=<p>]
                  login <username> <password>
                  logout
                  propose <word1> <word2> <word3> <word4>
                  gameinfo [gameId]        (omit for current game)
                  gamestats [gameId]
                  leaderboard [top=K | player=<name>]
                  mystats
                  help
                  quit
                """);
    }
}
