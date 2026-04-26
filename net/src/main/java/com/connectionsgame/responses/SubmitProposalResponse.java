package com.connectionsgame.responses;

import com.connectionsgame.abstract_class.Response;
import com.connectionsgame.requests.SubmitProposalRequest;

import javax.print.attribute.standard.MediaSize;
import java.util.List;

public class SubmitProposalResponse extends Response {

    private final String operation = "submitProposal";
    private final boolean correct;
    private final String theme; //null when wrong
    private final List<String> words; //null when wrong
    private final int mistakes; //0 when correct
    private final int currentScore;
    private final boolean gameOver;
    private final Boolean won; //null unless gameOver

    //on correct proposal
    public SubmitProposalResponse(String theme, List<String> words, int currentScore, boolean gameOver, Boolean won){
        super();
        this.correct = true;
        this.theme = theme;
        this.words = words;
        this.mistakes = 0;
        this.currentScore = currentScore;
        this.gameOver = gameOver;
        this.won = won;
    }

    //on wrong proposal
    public SubmitProposalResponse(int mistakes, int currentScore, boolean gameOver, Boolean won){
        super();
        this.correct = false;
        this.theme = null;
        this.words = null;
        this.mistakes = mistakes;
        this.currentScore = currentScore;
        this.gameOver = gameOver;
        this.won = won;
    }

    public boolean isCorrect() { return correct; }
    public String getTheme() { return theme; }
    public List<String> getWords() { return words; }
    public int getMistakes() { return mistakes; }
    public int getCurrentScore() { return currentScore; }
    public boolean isGameOver() { return gameOver; }
    public Boolean getWon() { return won; }

}
