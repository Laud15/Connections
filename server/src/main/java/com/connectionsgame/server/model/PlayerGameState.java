package com.connectionsgame.server.model;

//Tracks one player's in-progess state for the currently active game
//this object lives in memory inside GameSession
//When the game ends it is distilled into a GameResult and the User stats are updated
//Concurrency safe access is guarded by the lock on the containing GameSession

import java.util.ArrayList;
import java.util.List;

public class PlayerGameState {

    private final List<PuzzleGroup> correctGroups = new ArrayList<>();

    private int mistakes;
    private boolean finished; //a finished player can still be logged in but cannot send new proposals
    private boolean won; //true if finished and won (found 3 groups in time with < 4 mistakes)

    public synchronized int getBonus() { return correctGroups.size() * 6; } //bonus from correct groups: 6/12/18 for 1/2/3 correct groups
    public synchronized int getPenalty() { return mistakes * -4; }//-4 per mistake, maximum -16
    public synchronized int getCurrentScore() { return getBonus() + getPenalty(); }
    
    public synchronized boolean recordCorrectProposal(PuzzleGroup group) { //return true if the player has won (found 3 groups)
        correctGroups.add(group);

        if(correctGroups.size() >= 3) {
            finished = true;
            won = true;
        }
        return won;
    }

    public synchronized boolean recordWrongProposal() { //return true if the player has lost (4 mistakes)
        mistakes++;
        if (mistakes>=4){
            finished=true;
            won = false;
        }
        return finished && !won;
    }

    public synchronized List<PuzzleGroup> getCorrectGroups() { return new ArrayList<>(correctGroups); }
    public synchronized int getMistakes() { return mistakes; }
    public synchronized boolean isFinished() { return finished; }
    public synchronized boolean isWon() { return won; }

    public synchronized List<String> getRemainingWords(Puzzle puzzle){
        List<String> alreadyGrouped = new ArrayList<>();

        for(PuzzleGroup g : correctGroups) {
            alreadyGrouped.addAll(g.getWords());
        }

        return puzzle.getAllWords().stream().filter(w->!alreadyGrouped.contains(w)).toList();
    }

    public synchronized void markTimedOut(){
        if(!finished){
            finished = true;
            won = false;
        }
    }


}

