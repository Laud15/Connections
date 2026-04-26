package com.connectionsgame.client;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.logging.Logger;

/**
 * Manages the persistent NIO TCP connection from client to server.
 *
 * The spec requires the client to use NIO for this connection.
 * We use a single SocketChannel in blocking mode: this is valid NIO and much
 * simpler than a full Selector loop, which would be overkill for a single-threaded
 * CLI client that sends one request at a time and waits for one response.
 *
 * Message framing: each message is a JSON string terminated by '\n'.
 *
 * Note: the CLI loop is single-threaded (one request at a time), so no
 * synchronization is needed here.
 */
public class ServerConnection {

    private static final Logger LOG         = Logger.getLogger(ServerConnection.class.getName());
    private static final int    BUFFER_SIZE = 8192;
    private static final Gson   GSON        = new Gson();

    private final SocketChannel channel;
    private final ByteBuffer    readBuffer  = ByteBuffer.allocate(BUFFER_SIZE);

    // Accumulates bytes until a '\n' is found
    private final StringBuilder messageAccumulator = new StringBuilder();

    public ServerConnection(String host, int port) throws IOException {
        channel = SocketChannel.open();
        channel.configureBlocking(true); // blocking mode — simplest valid NIO usage
        channel.connect(new InetSocketAddress(host, port));
        LOG.info("ServerConnection: connected to " + host + ":" + port);
    }

    // ── Send ──────────────────────────────────────────────────────────────

    /**
     * Serialize an object to JSON and send it to the server, appending '\n'.
     * The object is typically one of the Request subclasses from the net module.
     */
    public void send(Object request) throws IOException {
        String json = GSON.toJson(request) + "\n";
        ByteBuffer buf = ByteBuffer.wrap(json.getBytes(StandardCharsets.UTF_8));
        while (buf.hasRemaining()) {
            channel.write(buf);
        }
    }

    /**
     * Send a raw JSON string (used when building requests manually in the CLI).
     */
    public void sendRaw(String json) throws IOException {
        String message = json.endsWith("\n") ? json : json + "\n";
        ByteBuffer buf = ByteBuffer.wrap(message.getBytes(StandardCharsets.UTF_8));
        while (buf.hasRemaining()) {
            channel.write(buf);
        }
    }

    // ── Receive ───────────────────────────────────────────────────────────

    /**
     * Block until a complete '\n'-terminated JSON response is received.
     *
     * @return the parsed JsonObject response from the server.
     * @throws IOException if the connection is closed or a read error occurs.
     */
    public JsonObject receive() throws IOException {
        // First check if we already have a complete message buffered
        int newlineIdx = messageAccumulator.indexOf("\n");
        while (newlineIdx == -1) {
            // Need more bytes
            readBuffer.clear();
            int bytesRead = channel.read(readBuffer);
            if (bytesRead == -1) throw new IOException("Server closed the connection");

            readBuffer.flip();
            messageAccumulator.append(
                    StandardCharsets.UTF_8.decode(readBuffer).toString()
            );
            newlineIdx = messageAccumulator.indexOf("\n");
        }

        // Extract the first complete message
        String json = messageAccumulator.substring(0, newlineIdx).trim();
        messageAccumulator.delete(0, newlineIdx + 1);
        return JsonParser.parseString(json).getAsJsonObject();
    }

    // ── Lifecycle ─────────────────────────────────────────────────────────

    public boolean isConnected() {
        return channel.isConnected();
    }


    /**
     * Block until a complete '\n'-terminated message is received, and return
     * the raw JSON string (without the trailing newline).
     *
     * Used by ClientMain together with ResponseDeserializer.parse() to get
     * a typed Response object instead of a raw JsonObject.
     *
     * @throws IOException if the connection is closed or a read error occurs.
     */
    public String receiveRaw() throws IOException {
        int newlineIdx = messageAccumulator.indexOf("\n");
        while (newlineIdx == -1) {
            readBuffer.clear();
            int bytesRead = channel.read(readBuffer);
            if (bytesRead == -1) throw new IOException("Server closed the connection");
            readBuffer.flip();
            messageAccumulator.append(
                    java.nio.charset.StandardCharsets.UTF_8.decode(readBuffer).toString()
            );
            newlineIdx = messageAccumulator.indexOf("\n");
        }
        String json = messageAccumulator.substring(0, newlineIdx).trim();
        messageAccumulator.delete(0, newlineIdx + 1);
        return json;
    }

    public void close() {
        try { channel.close(); } catch (IOException ignored) {}
    }
}
