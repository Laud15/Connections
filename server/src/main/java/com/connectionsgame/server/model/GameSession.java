package com.connectionsgame.server.model;

import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
//represents the single active game
//there is exactly one GameSession in memory at a time
// It holds the puzzle data, start/end timestamp, and per-player states
// concurrency strategy:
//              - playerStates is ConcurrentHashMap so reads from many threads are safe
//              - Operations that must be atomic synchronize on the playerGameState object for that specific player, not on this whole GameSession


public class GameSession {
    
    private final int gameId;
    private final Puzzle puzzle;
    private final Instant startTime;
    private final Instant endTime; //startTime + configuered game duration

    //username -> per-player progress; ConcurrentHashMap for safe concurrent reads/puts
    private final ConcurrentHashMap<String, PlayerGameState> playerStates = new ConcurrentHashMap<>();

    public GameSession(int gameId, Puzzle puzzle, Instant startTime, Instant endTime){
        this.gameId = gameId;
        this.puzzle = puzzle;
        this.startTime = startTime;
        this.endTime = endTime;
    }
    //returns the existing state for a player, or creates a new one if this is the first time this player touches this game
    public PlayerGameState getOrCreatePlayerState(String username) {
        return playerStates.computeIfAbsent(username, u->new PlayerGameState());
    }

    //returns the player state, or null if the player has never joined this game
    public PlayerGameState getPlayerState(String username){
        return playerStates.get(username);
    }

    //seconds remaining in the game
    public long getRemainingSeconds() {
        long remaining = endTime.getEpochSecond() - Instant.now().getEpochSecond();
        return Math.max(0, remaining);
    }

    public boolean isExpired() { return  Instant.now().isAfter(endTime); }

    public int getTotalPlayersJoined() { return playerStates.size(); }
    
    public int getPlayersStillPlaying() {
        return (int) playerStates.values().stream()
            .filter(s->{
                synchronized(s) { return !s.isFinished(); }
            })
            .count();
    }

    public int getPlayersFinished() {
        return (int) playerStates.values().stream()
            .filter(ps -> {
                synchronized(ps) { return ps.isFinished(); }
            })
            .count();
    }

    public int getPlayersWon() {
        return (int) playerStates.values().stream()
            .filter(ps -> {
                synchronized(ps) { return ps.isWon(); }
            })
            .count();
    }

    public double getAverageScore() {
        if(playerStates.isEmpty()) return 0.0;
        return playerStates.values().stream()
                .mapToInt(ps->{
                    synchronized(ps) { return ps.getCurrentScore(); }
                })
                .average()
                .orElse(0.0);
    }

    public int getGameId() { return gameId; }
    public Puzzle getPuzzle() { return puzzle; }
    public Instant getStartTime() { return startTime; }
    public Instant getEndTime() { return endTime; }

    public Map<String, PlayerGameState> getAllPlayerStates() { return Collections.unmodifiableMap(playerStates); }

    /**
     * Marks all players who haven't finished as timed out
     * Used when the game time expires to end the game for all players at once.
     */
    public void markAllPlayersTimedOut() {
        playerStates.values().forEach(ps -> {
            synchronized (ps) {
                if (!ps.isFinished()) {
                    ps.markTimedOut();
                }
            }
        });
    }
}
