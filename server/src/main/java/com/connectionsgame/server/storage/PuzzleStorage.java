package com.connectionsgame.server.storage;

import com.connectionsgame.server.model.Puzzle;
import com.connectionsgame.server.model.PuzzleGroup;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.logging.Logger;

/**
 * Loads puzzles on demand from Connections_Data.json by streaming
 * through the JSON array until the requested gameId is found.
 *
 * Only one puzzle object is ever in memory at a time.
 * The file is read at most once per game (every few minutes), so
 * the linear scan cost is completely negligible.
 */
public class PuzzleStorage {

    private static final Logger LOG = Logger.getLogger(PuzzleStorage.class.getName());

    private final Path dataFile;
    private final int  totalPuzzles;

    public PuzzleStorage(String dataFilePath) throws IOException {
        this.dataFile = Paths.get(dataFilePath);
        if (!Files.exists(dataFile)) {
            throw new IOException("Puzzle data file not found: " + dataFile);
        }
        this.totalPuzzles = countPuzzles();
        LOG.info("PuzzleStorage: found " + totalPuzzles + " puzzles in " + dataFile);
    }

    public int getTotalPuzzles() {
        return totalPuzzles;
    }

    /**
     * Streams through the JSON array until the puzzle with the given
     * gameId is found, then parses and returns it.
     *
     * @throws IOException if the gameId is not found or the file is unreadable.
     */
    public Puzzle loadPuzzle(int gameId) throws IOException {
        try (JsonReader reader = new JsonReader(new FileReader(dataFile.toFile()))) {
            reader.beginArray();
            while (reader.hasNext()) {
                // parse one element at a time as a JsonObject
                JsonObject obj = JsonParser.parseReader(reader).getAsJsonObject();
                if (obj.get("gameId").getAsInt() == gameId) {
                    return parsePuzzle(obj);
                }
                // not the one we need: the element is already consumed, just continue
            }
            reader.endArray();
        }
        throw new IOException("No puzzle with gameId " + gameId + " found in file");
    }

    // ── Internal ──────────────────────────────────────────────────────────

    /**
     * Counts the puzzles in the file by streaming through it once.
     * Only called at startup.
     */
    private int countPuzzles() throws IOException {
        int count = 0;
        try (JsonReader reader = new JsonReader(new FileReader(dataFile.toFile()))) {
            reader.beginArray();
            while (reader.hasNext()) {
                reader.skipValue(); // skip the whole object without parsing it
                count++;
            }
            reader.endArray();
        }
        return count;
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