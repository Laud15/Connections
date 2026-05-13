package com.connectionsgame.server;

import java.nio.channels.SocketChannel;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.ConcurrentHashMap;

//global registry of active client sessions
//this class is used when:
// a client connect to te server ( ,
// when a client disconnect himself,
// when there is an udp broadcast
//the concurrency is handled by ConcurrentHashMap
public class ClientSessionRegistry {
    private static final ConcurrentHashMap<SocketChannel, ClientSession> SESSIONS = new ConcurrentHashMap<>();

    private ClientSessionRegistry() {}

    public static void register(SocketChannel channel, ClientSession session){ SESSIONS.put(channel, session);}
    public static void unregister(SocketChannel channel) {SESSIONS.remove(channel);}
    public static ClientSession get(SocketChannel channel) { return SESSIONS.get(channel);}

    public static Collection<ClientSession> getAllSessions(){
        return Collections.unmodifiableCollection(SESSIONS.values());
    }

    public static ClientSession findByUsername(String username){
        return SESSIONS.values().stream().filter(s->username.equals(s.getLoggedInUserName())).findFirst().orElse(null);
    }
}
