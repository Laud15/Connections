package com.connectionsgame.server;

import com.connectionsgame.server.model.GameSession;
import com.google.gson.Gson;

import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Sends asynchronous UDP notifications from the server to individual clients.
 *
 * ── Why UDP for notifications ────────────────────────────────────────────────
 * The spec requires async notifications (e.g. game-over broadcast) to be sent
 * over UDP.  Each client tells the server its UDP listening port at login time.
 * The server stores this in the ClientSession registry and uses it here.
 *
 * ── Fire-and-forget ──────────────────────────────────────────────────────────
 * UDP is unreliable by design.  If a datagram is lost, the client will still
 * discover the game is over next time it polls requestGameInfo.
 * We do NOT retry; this is consistent with the course's requirements.
 *
 * ── Message format ───────────────────────────────────────────────────────────
 * Every UDP datagram is a JSON object with a "type" field, followed by payload
 * fields specific to that notification type.
 */
public class UdpNotifier {

    private static final Logger LOG  = Logger.getLogger(UdpNotifier.class.getName());
    private static final Gson   GSON = new Gson();

    private final DatagramSocket socket;

    public UdpNotifier(int localPort) throws SocketException {
        // Bind to the server's UDP port — not strictly required for sending,
        // but useful to identify traffic in network captures during testing.
        this.socket = new DatagramSocket(localPort);
        LOG.info("UdpNotifier: bound to UDP port " + localPort);
    }

    /**
     * Send a "game over" notification to every connected client.
     * game: The game that just ended.
     * The session registry (ClientSessionRegistry) is looked up via the static
     * accessor to avoid a circular dependency with NioServer.
     */
    public void broadcastGameOver(GameSession game) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("type", "gameOver");
        payload.put("gameId", game.getGameId());
        payload.put("remainingSeconds", 0);

        String json = GSON.toJson(payload);

        // Ask the registry for all currently connected clients' UDP addresses
        ClientSessionRegistry.getAllSessions().forEach(session -> {
            if (session.isLoggedIn()) {
                sendTo(session.getClientAddress(), session.getUdpPort(), json);
            }
        });
    }

    /**
     * Send an arbitrary JSON string to one specific client via UDP.
     * address:  The client's IP address (from the TCP connection).
     * port: The UDP port the client registered at login.
     * json: The JSON payload to send.
     */
    public void sendTo(InetAddress address, int port, String json) {
        if (address == null || port <= 0) {return;}
        byte[] data = json.getBytes(StandardCharsets.UTF_8);
        DatagramPacket packet = new DatagramPacket(data, data.length, address, port);
        try {
            socket.send(packet);
        } catch (Exception e) {
            LOG.warning("UdpNotifier: failed to send to " + address + ":" + port + " — " + e.getMessage());
        }
    }

    public void close() {
        socket.close();
    }
}
