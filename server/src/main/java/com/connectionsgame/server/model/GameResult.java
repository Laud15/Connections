package com.connectionsgame.server.model;

import java.util.Map;
import java.util.List;

//immutable snapshot of a completed game, persisted to disk as data/persistent/games/game_{id}.json

public class GameResult {

    //Summary of one player's performance
    // completed means that the player (won or lost) before time ran out
    public static class PlayerSummary {
        private final int correctGroups;
        private final int mistakes;
        private final int score;
        private final boolean won;
        private final boolean completed;

        public PlayerSummary(int correctGroups, int mistakes, int score, boolean won, boolean completed){
            this.correctGroups = correctGroups;
            this.mistakes = mistakes;
            this.score = score;
            this.won = won;
            this.completed = completed;
        }

        public int getCorrectGroups() { return correctGroups; }
        public int getMistakes() { return mistakes; }
        public int getScore() { return score; }
        public boolean isWon() { return won; }
        public boolean isCompleted() { return completed; }
    }

    private final int gameId;
    private final long start;
    private final long end;

    //full solution: category -> words, copied from Puzzle at game end
    private final Map<String, List<String>> solution;

    // per-player summary: username -> PlayerSummary
    private final Map<String, PlayerSummary> playerSummaries;

    public GameResult(int gameId,
                      long start,
                      long end,
                      Map<String, List<String>> solution,
                      Map<String, PlayerSummary> playerSummaries){

        this.gameId = gameId;
        this.start = start;
        this.end = end;
        this.solution = solution;
        this.playerSummaries = playerSummaries;
    }

    public int getGameId() { return gameId; }
    public long getStart() { return start; }
    public long getEnd() { return end; }
    public Map<String, List<String>> getSolution() { return solution; }
    public Map<String, PlayerSummary> getPlayerSummaries() { return playerSummaries; }

    public int getTotalPlayers() { return playerSummaries.size(); }

    public int getCompletedCount() {
        return (int) playerSummaries.values().stream().filter(ps -> ps.isCompleted()).count();
    }

    public int getWinCount() {
        return (int) playerSummaries.values().stream().filter(ps -> ps.isWon()).count();
    }

    public double getAverageScore() {
        if(playerSummaries.isEmpty()) return 0.0;
        return playerSummaries.values().stream()
                .mapToInt(ps -> ps.getScore())
                .average()
                .orElse(0.0);
    }


}
