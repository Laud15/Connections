package com.connectionsgame.client.config;

import java.util.Properties;
import java.io.FileInputStream;
import java.io.IOException;


public class ClientConfig {

    private final Properties props = new Properties();

    public ClientConfig(String configFilepath) throws IOException {
        try (FileInputStream fis = new FileInputStream(configFilepath)){
            props.load(fis);
        }
    }

    //each getter have a default value to avoid error in the properties file

    public String getServerHost() {return props.getProperty("server.host", "localhost");}

    public int getServerTcpPort() { return Integer.parseInt(props.getProperty("server.tcp.port", "12345"));}

    public int getLocalUdpPort() { return Integer.parseInt(props.getProperty("local.udp.port", "13000"));}

    public int getReadTimeoutMs() { return Integer.parseInt(props.getProperty("read.timeout.ms", "10000"));}


}
