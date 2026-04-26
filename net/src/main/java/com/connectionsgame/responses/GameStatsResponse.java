package com.connectionsgame.responses;

import com.connectionsgame.abstract_class.Response;

public class GameStatsResponse extends Response {

    private final String operation = "responseGameStats";
    private final int gameId;
    private final String gameStatus;

    private final Long remainingSeconds;
    private final Integer playersPlaying;
    private final Integer playersFinished;
    private final Integer playersWon;

    private final Integer totalPlayers;
    private final Integer completed;
    private final Integer won;
    private final Double averageScore;

    //constructor for in progress
    public GameStatsResponse(int gameId, long remainingSeconds, int playersPlaying, int playersFinished, int playersWon){
        super();
        this.gameId = gameId;
        this.gameStatus = "inProgress";
        this.remainingSeconds = remainingSeconds;
        this.playersPlaying = playersPlaying;
        this.playersFinished = playersFinished;
        this.playersWon = playersWon;
        this.totalPlayers = null;
        this.completed = null;
        this.won = null;
        this.averageScore = null;
    }

    //constructor for finished

    public GameStatsResponse(int gameId, int totalPlayers, int completed, int won, double averageScore){
        super();
        this.gameId = gameId;
        this.gameStatus = "finished";
        this.remainingSeconds = null;
        this.playersPlaying = null;
        this.playersFinished = null;
        this.playersWon = null;
        this.totalPlayers = totalPlayers;
        this.completed = completed;
        this.won = won;
        this.averageScore = averageScore;
    }

    public int getGameId() { return gameId; }
    public String getGameStatus() { return gameStatus; }
    public Long getRemainingSeconds() { return remainingSeconds; }
    public Integer getPlayersPlaying() { return playersPlaying; }
    public Integer getPlayersFinished() { return playersFinished; }
    public Integer getPlayersWon() { return playersWon; }
    public Integer getTotalPlayers() { return totalPlayers; }
    public Integer getCompleted() { return completed; }
    public Integer getWon() { return won; }
    public Double getAverageScore() { return averageScore; }

}
