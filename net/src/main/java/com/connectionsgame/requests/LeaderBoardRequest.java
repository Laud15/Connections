package com.connectionsgame.requests;

import com.connectionsgame.abstract_class.Request;

public class LeaderBoardRequest extends Request {

    //If set, return only the ranking position of this player.
    public final String playerName; // nullable

    // If > 0, return only the top-K players overall.
    public final int topPlayers;   // 0 means "all"

    public LeaderBoardRequest(String playerName, int topPlayers) {
        super("requestLeaderboard");
        this.playerName = playerName;
        this.topPlayers = topPlayers;
    }


    public String getPlayerName() { return playerName; }
    public int getTopPlayers() { return topPlayers; }
}
