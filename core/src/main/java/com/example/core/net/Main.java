package com.example.core.net;

public class Main {
    public static void main(String[] args) {
        HttpNettyServer server = new HttpNettyServer();
        ApiGroup apiGroup = server.getApiGroup();
        apiGroup.registerGetHandler("/hello1", () -> "hello world1!");
        apiGroup.registerGetHandler("/hello2", s -> "hello world2!");
        server.run();
    }
}
