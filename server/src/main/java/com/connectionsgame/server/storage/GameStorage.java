package com.connectionsgame.server.storage;

import com.connectionsgame.server.model.GameResult;
import com.google.gson.Gson;

import java.io.*;
import java.nio.file.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
import java.util.stream.Stream;

//persists completed game results to disk
//Each finished game is written to data/persistent/games/game_{id}.json
//finished games are cached in a concurrentHashMap so repeated queries for the same game hit use memory instead of disk

public class GameStorage {

    private static final Logger LOG = Logger.getLogger(GameStorage.class.getName());
    private static final Gson GSON = new Gson();

    private final Path gamesDir;

    private final ConcurrentHashMap<Integer, GameResult> cache = new ConcurrentHashMap<>();

    public GameStorage(String gamesDir) throws IOException {
        this.gamesDir = Paths.get(gamesDir);
        Files.createDirectories(this.gamesDir);
    }

    //persist a new completed game to disk and add it to the cache
    //called once per game, after all player scores have been finalised
    public void saveGame(GameResult result) throws IOException {
        Path path = gamesDir.resolve("game_" + result.getGameId() + ".json");
        try(Writer w = new FileWriter(path.toFile())){
            GSON.toJson(result, w);
        }
        cache.put(result.getGameId(), result);
        LOG.info("GameStorage: saved game " + result.getGameId());
    }
    //load the result of a specific completed game
    //returns null if no file exists for that id
    public GameResult loadGame(int gameId) {
        GameResult cached = cache.get(gameId);
        if(cached != null) return cached;

        Path path = gamesDir.resolve("game_" + gameId + ".json");
        if(!Files.exists(path)) return null;

        try (Reader r = new FileReader(path.toFile())){
            GameResult result = GSON.fromJson(r, GameResult.class);
            if(result != null) cache.put(gameId, result);
            return result;
        }catch (IOException e) {
            LOG.warning("GameStorage: could not load game " + gameId + ": " + e.getMessage());
            return null;
        }
    }

    //returns the highest game id found in games directory,
    // used at server startup to resume from where we left off,
    // returns 0 if no game file exist
    public int getLastGameId() {int maxId = 0;
        try{
            try(Stream<Path> stream = Files.list(gamesDir)) {
                for(Path p : stream.toList()) {
                    String name = p.getFileName().toString();

                    //check file's format
                    if(name.startsWith("game_") && name.endsWith(".json")){
                        try{
                            //extract number from the filename
                            String numberPart = name.substring(5, name.length() - 5);
                            int id = Integer.parseInt(numberPart);

                            if(id > maxId){
                                maxId = id;
                            }
                        }catch (NumberFormatException e){
                            //ignore malformed file
                        }
                    }
                }
                return maxId;
            }
        }catch (IOException e){
            LOG.warning("GameStorage could not scan games directory");
            return 0;
        }
    }

}
