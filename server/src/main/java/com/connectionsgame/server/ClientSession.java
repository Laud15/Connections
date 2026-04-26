package com.connectionsgame.server;

import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.net.InetSocketAddress;

//holds all state the server maintains about one connected TCP client
//each socketChannel accepted by the NIO selector gets its own ClientSession, which is stored as the attachment on the selectionKey

//State tracked:
//  - the socketChannel itself
//  - read buffer that accumulates bytes until a full '\n'-terminated message arrives
//  - write buffer for pending outbound data
//  - the logged-in username (null if not authenticated)
//  - the client's UDP port (registered at login, used by UdpNotifier)
public class ClientSession {

    private static final int BUFFER_SIZE = 8192;

    private final SocketChannel channel;
    private final InetAddress clientAddress;

    private final ByteBuffer  readBuffer = ByteBuffer.allocate(BUFFER_SIZE);
    private final ByteBuffer writeBuffer = ByteBuffer.allocate(BUFFER_SIZE * 4);

    private volatile String loggedInUserName;
    private volatile int udpPort;

    public  ClientSession(SocketChannel channel) throws java.net.SocketException {
        this.channel = channel;
        InetSocketAddress remote = (InetSocketAddress) channel.socket().getRemoteSocketAddress();
        this.clientAddress = (remote != null) ? remote.getAddress() : null;
     }

     public boolean isLoggedIn() { return loggedInUserName != null; }
     public String getLoggedInUserName() { return loggedInUserName; }

    public void login(String username, int udpPort){
        this.loggedInUserName = username;
        this.udpPort = udpPort;
    }

    public void logout(){
        this.loggedInUserName = null;
        this.udpPort = 0;
    }

    public SocketChannel getChannel() { return channel; }
    public ByteBuffer getReadBuffer() { return readBuffer; }
    public ByteBuffer getWriteBuffer() { return writeBuffer; }
    public InetAddress getClientAddress() { return clientAddress; }
    public int getUdpPort() { return udpPort; }
}
