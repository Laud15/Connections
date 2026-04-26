package com.connectionsgame.server.storage;

import com.connectionsgame.server.model.Puzzle;
import com.connectionsgame.server.model.PuzzleGroup;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.logging.Logger;

//Provides in-memory access to all puzzles stored in a single JSON file.
//At startup, the entire file is parsed once and all puzzles are loaded into a HashMap (gameId -> Puzzle).
//REMEMBER: ALL DATA ARE LOAD IN MEMORY


public class PuzzleStorage {

    private static final Logger LOG = Logger.getLogger(PuzzleStorage.class.getName());
    private static final Gson GSON = new Gson();
    private final Path dataFile;
    private final Map<Integer, Puzzle> cache = new HashMap<>(); // cache: gameId -> Puzzle

    public PuzzleStorage(String dataFilePath) throws IOException {
        this.dataFile = Paths.get(dataFilePath);

        if (!Files.exists(dataFile)) {
            throw new IOException("Puzzle data file not found: " + dataFile);
        }

        loadAllPuzzles();
        LOG.info("PuzzleStorage: loaded " + cache.size() + " puzzles into memory.");
    }

    public int getTotalPuzzles() { return cache.size(); }

    public Puzzle loadPuzzle(int gameId) throws IOException {
        Puzzle p = cache.get(gameId);
        if (p == null) {
            throw new IOException("No puzzle with gameId " + gameId);
        }
        return p;
    }

    private void loadAllPuzzles() throws IOException {
        try (Reader r = new FileReader(dataFile.toFile())) {

            JsonArray arr = JsonParser.parseReader(r).getAsJsonArray();

            for (var elem : arr) {
                Puzzle p = parsePuzzle(elem.getAsJsonObject());
                cache.put(p.getGameId(), p);
            }
        }
    }

    private Puzzle parsePuzzle(JsonObject obj) {

        int gameId = obj.get("gameId").getAsInt();
        List<PuzzleGroup> groups = new ArrayList<>();

        for (var element : obj.getAsJsonArray("groups")) {
            JsonObject g = element.getAsJsonObject();
            String theme = g.get("theme").getAsString();
            List<String> words = new ArrayList<>();

            for (var w : g.getAsJsonArray("words")) {
                words.add(w.getAsString().toUpperCase());
            }
            groups.add(new PuzzleGroup(theme, words));
        }
        return new Puzzle(gameId, groups);
    }
}