package com.connectionsgame.requests;

import com.connectionsgame.abstract_class.Request;


 //No fields needed: the server derives the user from the logged-in session.


public class PlayerStatsRequest extends Request{

    public PlayerStatsRequest(){
        super("requestPlayerStats");
    }

}
