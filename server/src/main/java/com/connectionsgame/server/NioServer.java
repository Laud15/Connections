package com.connectionsgame.server;

import com.connectionsgame.server.storage.UserStorage;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.Queue;
import java.util.concurrent.*;
import java.util.logging.Logger;

/**
 * Non-blocking TCP server using Java NIO.
 *
 * ── Architecture ─────────────────────────────────────────────────────────────
 *
 *  Single Selector thread (this class's run() method):
 *    - Accepts new connections.
 *    - Reads bytes from ready channels into the ClientSession's read buffer.
 *    - Detects complete messages (delimited by '\n').
 *    - Submits each complete message to the ThreadPoolExecutor as a RequestHandler.
 *    - Writes pending response bytes back to channels.
 *
 *  ThreadPoolExecutor (worker threads):
 *    - Runs RequestHandler instances (pure business logic, no I/O).
 *    - When a handler finishes, it calls the WriteCallback, which adds the
 *      response to a per-session pending-writes queue and wakes up the Selector.
 *    - Worker threads NEVER write to a SocketChannel directly — only the
 *      Selector thread does.  This eliminates all I/O concurrency bugs.
 *
 * ── Thread pool sizing ───────────────────────────────────────────────────────
 *   coreSize  = CPU cores       (always alive, handle steady-state load)
 *   maxSize   = CPU cores × 4   (burst capacity without unbounded growth)
 *   queue     = bounded (200)   + CallerRunsPolicy
 *
 *   CallerRunsPolicy means: if the queue is full AND all max threads are busy,
 *   the Selector thread itself runs the task.  This naturally throttles new
 *   I/O reads (backpressure) without dropping requests or throwing exceptions.
 *   It's the recommended policy for compute-bound tasks with a known max concurrency.
 *
 * ── Message framing ──────────────────────────────────────────────────────────
 *   Each message is a single JSON object terminated by a newline '\n'.
 *   The read buffer accumulates bytes until '\n' is found, then the complete
 *   JSON string is extracted and submitted to the thread pool.
 */
public class NioServer implements Runnable {

    private static final Logger LOG = Logger.getLogger(NioServer.class.getName());

    private final int tcpPort;
    private final GameManager gameManager;
    private final UserStorage userStorage;
    private final ExecutorService threadPool;
    private Selector selector;

    /**
     * A queue of (session, responseJson) pairs accumulated by worker threads,
     * drained by the Selector thread on each wakeup.
     * ConcurrentLinkedQueue is lock-free for the common case.
     */
    private final Queue<PendingWrite> pendingWrites = new ConcurrentLinkedQueue<>();

    public NioServer(int tcpPort, int poolCore, int poolMax,
                     long keepAliveSeconds,
                     int queueCapacity,
                     GameManager gameManager,
                     UserStorage userStorage) {

        this.tcpPort = tcpPort;
        this.gameManager = gameManager;
        this.userStorage = userStorage;

        // Thread pool configuration (like FixedThreadPool, but with burst scaling):
        // - corePoolSize = poolCore: threads always alive, handle steady-state load
        // - maxPoolSize = poolMax > poolCore: temporary extra threads during traffic spikes
        //   (temporary threads exit after keepAliveSeconds idle time)
        // - ArrayBlockingQueue: bounded capacity; excess tasks are rejected (AbortPolicy by default)
        this.threadPool = new ThreadPoolExecutor(
                poolCore,
                poolMax,  // Allows temporary scaling during bursts (above core pool size)
                keepAliveSeconds,
                TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(queueCapacity)
        );
    }

    // ── Main Selector loop ────────────────────────────────────────────────

    @Override
    public void run() {
        try {
            selector = Selector.open();
            ServerSocketChannel serverChannel = ServerSocketChannel.open();
            serverChannel.configureBlocking(false);
            serverChannel.bind(new InetSocketAddress(tcpPort));
            serverChannel.register(selector, SelectionKey.OP_ACCEPT);
            LOG.info("NioServer: listening on TCP port " + tcpPort);

            while (!Thread.currentThread().isInterrupted()) {
                // Drain pending responses from worker threads before selecting
                drainPendingWrites();

                // Block until at least one channel is ready (or woken by pending writes)
                selector.select(500); // 500ms timeout so we check interruption regularly

                Iterator<SelectionKey> keys = selector.selectedKeys().iterator();
                while (keys.hasNext()) {
                    SelectionKey key = keys.next();
                    keys.remove();

                    if (!key.isValid()) {continue;}

                    if (key.isAcceptable()) {accept(serverChannel, key);}
                    else if (key.isReadable()) {read(key);}
                    else if (key.isWritable()) {write(key);}
                }
            }
        } catch (IOException e) {
            LOG.severe("NioServer: fatal error — " + e.getMessage());
        } finally {
            shutdown();
        }
    }

    // ── Accept ────────────────────────────────────────────────────────────

    private void accept(ServerSocketChannel serverChannel, SelectionKey key) throws IOException {
        SocketChannel client = serverChannel.accept();
        if (client == null) return;

        client.configureBlocking(false);
        ClientSession session = new ClientSession(client);
        ClientSessionRegistry.register(client, session);

        // Register for READ, attach the session so we can retrieve it on later events
        client.register(selector, SelectionKey.OP_READ, session);
        LOG.info("NioServer: accepted connection from " + client.getRemoteAddress());
    }

    // ── Read ──────────────────────────────────────────────────────────────

    private void read(SelectionKey key) {
        ClientSession session = (ClientSession) key.attachment();
        SocketChannel channel = session.getChannel();
        ByteBuffer    buf     = session.getReadBuffer();

        int bytesRead;
        try {
            bytesRead = channel.read(buf);
        } catch (IOException e) {
            LOG.info("NioServer: client disconnected (read error): " + e.getMessage());
            closeChannel(key, session);
            return;
        }

        if (bytesRead == -1) {
            // Client closed the connection
            closeChannel(key, session);
            return;
        }

        // Look for complete messages (each terminated by '\n')
        buf.flip();
        byte[] array = buf.array();
        int limit = buf.limit();
        int start = 0;

        for (int i = 0; i < limit; i++) {
            if (array[i] == '\n') {
                // Found a complete message: bytes [start, i)
                String message = new String(array, start, i - start, StandardCharsets.UTF_8).trim();
                start = i + 1;

                if (!message.isEmpty()) {
                    // Submit to thread pool; the WriteCallback will queue the response
                    RequestHandler handler = new RequestHandler(
                            message,
                            session,
                            gameManager,
                            userStorage,
                            (cs, response) -> this.enqueueResponse(cs, response)
                    );
                    try {
                        threadPool.submit(handler);
                    } catch (RejectedExecutionException e) {
                        LOG.severe("NioServer: Task rejected by thread pool: " + e.getMessage());
                    }
                }
            }
        }

        // Compact: keep any incomplete message bytes for the next read
        buf.position(start);
        buf.compact();
    }

    // ── Write ─────────────────────────────────────────────────────────────

    //Finishes a partial write left over from drainPendingWrites().
    // Called by the Selector only when OP_WRITE is active, which happens only when a previous channel.write() could not send all bytes at once
    private void write(SelectionKey key) {//
        ClientSession session = (ClientSession) key.attachment();
        SocketChannel channel = session.getChannel();
        ByteBuffer buf = session.getWriteBuffer();

        buf.flip();
        try {
            channel.write(buf);
        } catch (IOException e) {
            LOG.info("NioServer: write error, closing connection: " + e.getMessage());
            closeChannel(key, session);
            return;
        }
        buf.compact();

        // If everything was written, stop watching for WRITE events
        if (buf.position() == 0) {
            key.interestOps(SelectionKey.OP_READ);
        }
    }

    // ── Pending writes (cross-thread handoff) ─────────────────────────────

    /**
     * Called by worker threads (RequestHandler) when a response is ready.
     * Adds it to the lock-free queue and wakes the Selector.
     */
    private void enqueueResponse(ClientSession session, String responseJson) {
        pendingWrites.add(new PendingWrite(session, responseJson));//fill pendigWrites with the response of the request
        selector.wakeup(); // wake up selector.select() so it drains the queue immediately
    }

    /**
     * Drains all pending write tasks accumulated since the last Selector wakeup.
     * Called only from the Selector thread — no locking needed.
     */
    private void drainPendingWrites() {
        PendingWrite pw;
        while ((pw = pendingWrites.poll()) != null) {//poll return null if the queue is empty otherwise return the head of the queue
            ClientSession session = pw.session;
            byte[] bytes = pw.responseJson.getBytes(StandardCharsets.UTF_8);
            ByteBuffer writeBuf = session.getWriteBuffer();

            // Try to write directly to the channel
            try {
                SocketChannel channel = session.getChannel();
                if (!channel.isOpen()) continue;

                ByteBuffer data = ByteBuffer.wrap(bytes);
                channel.write(data);

                if (data.hasRemaining()) {
                    // Partial write: buffer the rest and register for OP_WRITE
                    writeBuf.put(data);
                    SelectionKey key = channel.keyFor(selector);
                    if (key != null && key.isValid()) {
                        key.interestOps(SelectionKey.OP_READ | SelectionKey.OP_WRITE);
                    }
                }
            } catch (IOException e) {
                LOG.warning("NioServer: error writing response: " + e.getMessage());
            }
        }
    }

    // ── Cleanup ───────────────────────────────────────────────────────────

    private void closeChannel(SelectionKey key, ClientSession session) {
        try {
            key.cancel();
            session.getChannel().close();
            ClientSessionRegistry.unregister(session.getChannel());
        } catch (IOException ignored) {}
    }

    private void shutdown() {
        LOG.info("NioServer: shutting down thread pool...");
        threadPool.shutdown();
        try {
            if (!threadPool.awaitTermination(5, TimeUnit.SECONDS)) {
                threadPool.shutdownNow();
            }
        } catch (InterruptedException e) {
            threadPool.shutdownNow();
            Thread.currentThread().interrupt();
        }
        try { if (selector != null) selector.close(); } catch (IOException ignored) {}
    }

    // ── Inner types ───────────────────────────────────────────────────────

    /** A response queued by a worker thread for the Selector thread to write. */
    private record PendingWrite(ClientSession session, String responseJson) {}
}
