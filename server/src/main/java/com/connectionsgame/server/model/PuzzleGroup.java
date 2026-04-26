package com.connectionsgame.server.model;

// One of the four thematic groups within a puzzle.
// Mirrors the JSON structure of Connections_Data.json exactly:
//      { "theme": "WET WEATHER", "words": ["SNOW","HAIL","RAIN","SLEET"] }
//The field is named "theme" in the source file (not "category").


import java.util.List;

public class PuzzleGroup {

    private String theme;
    private  List<String> words;

    public PuzzleGroup(String theme, List<String> words) {
        this.theme = theme;
        this.words = words;
    }

    public String getTheme() { return theme; }
    public List<String> getWords() { return words; }
}
