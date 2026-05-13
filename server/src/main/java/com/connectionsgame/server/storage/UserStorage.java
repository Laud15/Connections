package com.connectionsgame.server.storage;

//each user has its own file: data/persistent/users/{id}.json

import com.google.gson.Gson;
import com.connectionsgame.server.model.User;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

//the UserStorage class use an IndexFile that contains the couple of {username:id}
//the IndexFile is necessary to associate the Username to the id that give the name of user's file
//an alternative is use only the username to name to identifier the user's file, but it would have required the rename of the file
//the save operation are done inside synchronized method/block to prevent race condition

public class UserStorage {

    private static final Logger LOG = Logger.getLogger(UserStorage.class.getName());
    private static final Gson GSON = new Gson();
    private final Path userDir;
    private final Path indexFile;

    //id->user (lazy, load from disk on demand)
    //the program doesn't have a way to remove a user so a possible upgrade will be to define a cut policy
    private final ConcurrentHashMap<Integer, User> cache = new ConcurrentHashMap<>();
    //username->id (load at the start, always complete)
    private final ConcurrentHashMap<String, Integer> usernameToId = new ConcurrentHashMap<>();
    //incremental id generator, initialized at the start with the max. value found in the index
    private final AtomicInteger idGenerator = new AtomicInteger(0);

    public UserStorage(String userDir) throws IOException {
        this.userDir = Paths.get(userDir);
        this.indexFile = this.userDir.resolve("index.json");
        Files.createDirectories(this.userDir);
        loadIndex();
        LOG.info("UserStorage: initialized, user dir = " + this.userDir);
    }

    //load the index that contains all the {username: id}s
    private void loadIndex() throws IOException{
        if(!Files.exists(indexFile)) {return;}
        try (Reader r = new FileReader(indexFile.toFile())){
            JsonObject obj = JsonParser.parseReader(r).getAsJsonObject();
            for(String username : obj.keySet()) {
                int id = obj.get(username).getAsInt();
                usernameToId.put(username, id);
                idGenerator.updateAndGet(current -> Math.max(current, id));
            }
        }
    }

    private void saveIndex() throws IOException {
        JsonObject obj = new JsonObject();
        usernameToId.forEach((username, id)->obj.addProperty(username, id));
        try(Writer w = new FileWriter(indexFile.toFile())){
            GSON.toJson(obj, w);
        }
    }

    public synchronized boolean register(String username, String password) throws IOException{
            if (usernameToId.containsKey(username)) {return false;}
            int id = idGenerator.incrementAndGet();
            User newUser = new User(id, username, password);
            cache.put(id, newUser);
            usernameToId.put(username, id);
            saveUser(newUser);
            saveIndex();
            return true;
    }

    public User authenticate(String username, String password){
        User user = getByUsername(username);
        if(user == null) {return null;}
        if(!password.equals(user.getPassword())) {return null;}
        return user;
    }

    public String updateCredentials(String oldName, String oldPsw, String newName, String newPsw) throws IOException {
        User user = authenticate(oldName, oldPsw);
        if(user == null) {return "Wrong username or password";}

        synchronized (this) {
            if (newName != null && !newName.equals(oldName)) {
                if (usernameToId.containsKey(newName)) {return "Username '" + newName + "' is already taken";}
                usernameToId.remove(oldName);
                usernameToId.put(newName, user.getUser_id());
                synchronized (user) { user.setUsername(newName); }
                saveIndex();
            }
            synchronized (user) {
                if(newPsw != null) {
                        user.setPassword((newPsw));
                        saveUser(user);
                }
            }
        }
        return null;
    }

    public User getByUsername(String username) {
        Integer id = usernameToId.get(username);
        if (id == null) {return null;}
        return getById(id);
    }

    public User getById(int id){
        // ✅ FIXED: Use computeIfAbsent for atomic, single-load guarantee
        // If already cached → returns immediately
        // If not cached → loads EXACTLY ONCE, even with 100 concurrent threads
        return cache.computeIfAbsent(id, this::loadFromDiskNoCache);
    }

    public List<User> getLeaderboard() {
        usernameToId.values().forEach(id ->
                cache.computeIfAbsent(id, this::loadFromDiskNoCache)
        );
        List<User> sorted = new ArrayList<>(cache.values());
        sorted.sort(Comparator.comparingInt((User user)->user.getTotalScore()).reversed());
        return sorted;
    }

    public void saveUser(User user) throws IOException {
        Path path = userDir.resolve(user.getUser_id() + "_user.json");
        try(Writer w = new FileWriter(path.toFile())) {
            GSON.toJson(user, w);
        }
    }

    public void saveAll(){
        cache.values().forEach(u -> {
            try { saveUser(u);}
            catch(IOException e){
                LOG.warning("Could not save user id=" + u.getUser_id() + "; " + e.getMessage());
            }
        });
    }

    /**
     * Load a user from disk WITHOUT putting it in cache.
     * The caller (computeIfAbsent) handles the atomic put.
     * This ensures only ONE disk read happens per uncached user, regardless of concurrent threads.
     */
    private User loadFromDiskNoCache(int id) {
        Path path = userDir.resolve(id + "_user.json");
        if (!Files.exists(path)) return null;
        try (Reader r = new FileReader(path.toFile())){
            return GSON.fromJson(r, User.class);
        } catch (IOException e){
            LOG.warning("could not load user id = "+ id + ": " + e.getMessage());
            return null;

        }
    }

}
