package com.connectionsgame.client;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutionException;
import java.util.logging.Logger;

/**
 * Runs as a daemon thread and listens for UDP datagrams sent by the server.
 *
 * Currently handles:
 *   - "gameOver": prints a notification so the player knows the current game ended.
 *
 * The listener is started once per client session and runs until the JVM exits.
 * Because it's a daemon thread, it does not prevent the JVM from shutting down
 * when the main (CLI) thread ends.
 */
public class UdpListener extends Thread {

    private static final Logger LOG = Logger.getLogger(UdpListener.class.getName());
    private static final int BUFFER_SIZE = 4096;

    private final DatagramSocket socket;
    private volatile boolean running = true;

    public UdpListener(int localPort) throws SocketException {
        super("udp-listener");
        setDaemon(true); // does not block JVM shutdown
        this.socket = new DatagramSocket(localPort);
    }

    @Override
    public void run() {
        try  {
            socket.setSoTimeout(1000); // check 'running' flag every second
            LOG.info("UdpListener: bound to UDP port " + socket.getLocalPort());

            byte[] buf = new byte[BUFFER_SIZE];
            DatagramPacket packet = new DatagramPacket(buf, buf.length);

            while (running) {
                try {
                    socket.receive(packet);
                    String json = new String(packet.getData(), 0, packet.getLength(), StandardCharsets.UTF_8);
                    handleNotification(json);
                } catch (SocketTimeoutException ingnored) { }
            }
        } catch (Exception e) {
            if (running) LOG.warning("UdpListener: error — " + e.getMessage());
        }finally {
            socket.close();
        }
    }

    private void handleNotification(String json) {
        try {
            JsonObject obj = JsonParser.parseString(json).getAsJsonObject();
            String type = obj.has("type") ? obj.get("type").getAsString() : "";

            switch (type) {
                case "gameOver" -> {
                    int gameId = obj.has("gameId") ? obj.get("gameId").getAsInt() : -1;
                    // Print on a new line so it doesn't clobber the current input prompt
                    System.out.println("\n[SERVER] Game #" + gameId + " has ended. Use 'gameinfo' to see your result.");
                    System.out.print("> "); // re-print prompt
                }
                default -> LOG.fine("UdpListener: unknown notification type: " + type);
            }
        } catch (Exception e) {
            LOG.warning("UdpListener: could not parse notification: " + e.getMessage());
        }
    }

    /** Signal the listener to stop gracefully. */
    public void stopListening() {
        running = false;
    }

    public int getLocalPort() {return socket.getLocalPort();}
}
