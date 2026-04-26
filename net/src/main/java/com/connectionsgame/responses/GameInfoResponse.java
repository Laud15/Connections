package com.connectionsgame.responses;

import com.connectionsgame.abstract_class.Response;
import java.util.List;
import java.util.Map;

public class GameInfoResponse extends Response {

    private final String operation = "responseGameInfo";
    private final int gameId;
    private final String gameStatus;

    private final Long remainingSeconds;
    private final List<String> correctGroups;
    private final List<String> remainingWords;
    private final Integer mistakes;
    private final Integer currentScore;

    private final Map<String, List<String>> solution;
    private final Integer correctCount;
    private final Integer finalScore;

    //constructor for an in-progress game
    public GameInfoResponse(int gameId, long remainingSeconds, List<String> correctGroups, List<String> remainingWords, int mistakes, int currentScore) {
        super();
        this.gameId = gameId;
        this.gameStatus = "inProgress";
        this.remainingSeconds = remainingSeconds;
        this.correctGroups = correctGroups;
        this.remainingWords = remainingWords;
        this.mistakes = mistakes;
        this.currentScore = currentScore;
        this.solution = null;
        this.correctCount = null;
        this.finalScore = null;
    }

    //constructor for a finished game
    public GameInfoResponse(int gameId, Map<String, List<String>> solution, int correctCount, int mistakes, int finalScore){
        super();
        this.gameId = gameId;
        this.gameStatus = "finished";
        this.remainingSeconds = null;
        this.correctGroups = null;
        this.remainingWords = null;
        this.solution = solution;
        this.correctCount = correctCount;
        this.mistakes = mistakes;
        this.currentScore = null;
        this.finalScore = finalScore;
    }

    public int getGameId() { return gameId; }
    public String getGameStatus() { return gameStatus; }
    public Long getRemainingSeconds() { return remainingSeconds; }
    public List<String> getCorrectGroups() {return correctGroups; }
    public List<String> getRemainingWords() { return remainingWords; }
    public Integer getMistakes() { return mistakes; }
    public Integer getCurrentScore() { return currentScore; }
    public Map<String, List<String>> getSolution() { return solution; }
    public Integer getCorrectCount() { return correctCount; }
    public Integer getFinalScore(){ return finalScore; }

}
