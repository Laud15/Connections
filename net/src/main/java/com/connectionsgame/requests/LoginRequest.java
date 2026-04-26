package com.connectionsgame.requests;

import com.connectionsgame.abstract_class.Request;

public class LoginRequest extends Request {

    private final String username;
    private final String pwd;
    private final int udpPort;

    public LoginRequest(String username, String pwd, int udpPort){
        super("requestLogin");
        this.username = username;
        this.pwd = pwd;
        this.udpPort = udpPort;
    }

    public String getUsername() { return username;}
    public String getPwd() { return pwd; }
    public int getUdpPort() { return udpPort; }
}
