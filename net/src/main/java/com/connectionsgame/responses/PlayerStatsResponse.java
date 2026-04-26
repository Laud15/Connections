package com.connectionsgame.responses;

import com.connectionsgame.abstract_class.Response;
import com.connectionsgame.requests.PlayerStatsRequest;

public class PlayerStatsResponse extends Response {

    private final String operation = "responsePlayerStats";
    private final int puzzlesCompleted;
    private final double winRate;
    private final double lossRate;
    private final int currentStreak;
    private final int maxStreak;
    private final int perfectPuzzles;
    private final int[] mistakeHistogram;

    public PlayerStatsResponse(int puzzlesCompleted, double winRate, double lossRate, int currentStreak, int maxStreak, int perfectPuzzles, int[] mistakeHistogram){

        super();
        this.puzzlesCompleted = puzzlesCompleted;
        this.winRate = winRate;
        this.lossRate = lossRate;
        this.currentStreak = currentStreak;
        this.maxStreak = maxStreak;
        this.perfectPuzzles = perfectPuzzles;
        this.mistakeHistogram = mistakeHistogram;
    }

    public int getPuzzlesCompleted() { return this.puzzlesCompleted; }
    public double getWinRate() { return this.winRate; }
    public double getLossRate() { return this.lossRate; }
    public int getCurrentStreak() { return this.currentStreak; }
    public int getMaxStreak() { return this.maxStreak; }
    public int getPerfectPuzzles() { return this.perfectPuzzles; }
    public int[] getMistakeHistogram() { return this.mistakeHistogram; }
}
