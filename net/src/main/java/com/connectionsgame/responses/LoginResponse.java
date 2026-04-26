package com.connectionsgame.responses;

import com.connectionsgame.abstract_class.Response;

import java.util.Currency;
import java.util.List;

public class LoginResponse extends Response{

    private final String operation = "loginResponse";
    private final String message;
    private final Integer gameId; //null if no acrive game
    private final Long remainingSeconds; //null if no active game
    private final List<String> words; //shuffled remainig words
    private final List<String> correctGroups; //theme name already found
    private final Integer mistakes;
    private final Integer currentScore;


    //constructor when a game is currently active
    public LoginResponse(String message, int gameId, long remainingSeconds, List<String> words, List<String> correctGroups, int mistakes, int currentScore){
        super();
        this.message = message;
        this.gameId = gameId;
        this.remainingSeconds = remainingSeconds;
        this.words = words;
        this.correctGroups = correctGroups;
        this.mistakes = mistakes;
        this.currentScore = currentScore;
    }

    //constructor when no game is currently active
    public LoginResponse(String message){
        super();
        this.message = message;
        this.gameId           = null;
        this.remainingSeconds = null;
        this.words            = null;
        this.correctGroups    = null;
        this.mistakes         = null;
        this.currentScore     = null;
    }

    public String getMessage() { return message; }
    public Integer getGameId() { return gameId; }
    public Long getRemainingSeconds() { return remainingSeconds; }
    public List<String> getWords() { return words; }
    public List<String> getCorrectGroups() { return correctGroups; }
    public Integer getMistakes() { return mistakes; }
    public Integer getCurrentScore() { return currentScore; }
}

