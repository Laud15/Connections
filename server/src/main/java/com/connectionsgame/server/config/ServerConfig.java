package com.connectionsgame.server.config;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;


public class ServerConfig {

    private final Properties props = new Properties();

    public ServerConfig(String configFilePath) throws IOException {
        try(FileInputStream fis = new FileInputStream(configFilePath)) {
            props.load(fis);
        }
    }

    public int getTcpPort() { return Integer.parseInt(props.getProperty("tcp.port", "12345"));}

    public int getUdpPort() { return Integer.parseInt(props.getProperty("udp.port", "12346"));}

    public int getThreadPoolCore(){
        int def = Runtime.getRuntime().availableProcessors();
        return Integer.parseInt(props.getProperty("thread.pool.core", String.valueOf(def)));
    }

    public int getThreadPoolMax(){
        int def = Runtime.getRuntime().availableProcessors() * 4;
        return Integer.parseInt(props.getProperty("thread.pool.max", String.valueOf(def)));
    }

    public long getThreadKeepAliveSeconds() {
        return Long.parseLong(props.getProperty("thread.keepalive.seconds", "60"));
    }

    public int getThreadQueueCapacity() { return Integer.parseInt(props.getProperty("thread.queue.capacity", "200")); }

    public int getGameDurationMinutes() { return Integer.parseInt(props.getProperty("game.duration.minutes", "5")); }

    public String getPuzzleDataFile() { return props.getProperty("puzzle.data.file", "./data/Connections_Data.json"); }

    public String getUserDir() { return props.getProperty("users.dir", "./data/persistent/users"); }

    public String getGamesDir() { return props.getProperty("games.dir", "./data/persistent/games");}

    public int getPersistenceIntervalSeconds() {
        return Integer.parseInt(props.getProperty("persistence.interval.seconds", "30"));
    }
}
