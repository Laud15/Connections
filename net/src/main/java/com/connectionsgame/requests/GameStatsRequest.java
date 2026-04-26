package com.connectionsgame.requests;

import com.connectionsgame.abstract_class.Request;


 //Use gameId == -1 for the current game.


public class GameStatsRequest extends Request{

    private final int id;

    public GameStatsRequest(int id){
        super("requestGameStats");
        this.id = id;
    }

    public int getId() { return id; }
}
