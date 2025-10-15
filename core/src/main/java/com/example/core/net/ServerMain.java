package com.example.core.net;

public class ServerMain {
    public static void main(String[] args) {
        int port = NettyServerConstant.PORT;
        ReactorServer server = new ReactorServer(port);
        server.run();
    }
}

