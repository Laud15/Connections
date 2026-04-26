package com.connectionsgame.requests;

import java.util.Objects;
import com.connectionsgame.abstract_class.Request;


 //If gameId == -1 the server treats this as "give me info about the current game".
 //Otherwise gameId refers to a historical game.
 //The response differs based on whether the game is still in progress or finished:
 // - In progress: remaining time, correct groups found, remaining words, errors, current score.
 // - Finished: full solution (all 4 groups), correct count, error count, final score.


public class GameInfoRequest extends Request{

    private final int id;

    public GameInfoRequest(int id){
        super("requestGameInfo");
        this.id = id;
    }

    public int getId() {
        return id;
    }

}
