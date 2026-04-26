package com.connectionsgame.server.model;

import java.util.ArrayList;
import java.util.List;
/**
 * Represents one Connections puzzle loaded from Connections_Data.json.
 * JSON structure in the source file:
 * {
 *   "gameId": 0,
 *   "groups": [
 *     { "theme": "WET WEATHER", "words": ["SNOW","HAIL","RAIN","SLEET"] },
 *     ...
 *   ]
 * }
 * This object is loaded on demand from the data file using a byte-offset index
 * and lives in memory only for the duration of the active game.
 */


public class Puzzle {

    private int gameId;
    private List<PuzzleGroup> groups;

    public Puzzle(int gameId, List<PuzzleGroup> groups) {
        this.gameId = gameId;
        this.groups = groups;
    }

    public int getGameId() { return gameId; }
    public List<PuzzleGroup> getGroups() { return groups; }

    // collect all 16 words from the 4 groups into a flat list
    public List<String> getAllWords() {
        List<String> words= new ArrayList<>();
        for(PuzzleGroup pg : groups){
          for(String w : pg.getWords()){
              words.add(w);
          }
        }
        return words;
    }

    /**
     * Returns the group whose word set matches the proposal exactly,
     * or null if no group matches.
     * Used by the server to validate a player's proposal.
     */
    public PuzzleGroup findMatchingGroup(List<String> proposal) {
        for(PuzzleGroup group : groups) {
            if(group.getWords().containsAll(proposal) && proposal.containsAll(group.getWords())){
                return group;
            }
        }
        return null;
    }

}
