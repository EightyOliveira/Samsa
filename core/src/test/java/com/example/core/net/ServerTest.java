package com.example.core.net;

import org.junit.jupiter.api.Test;

public class ServerTest {

    @Test
    public void start() {
        ReactorServer server = new ReactorServer(8080);

        ApiGroup apiGroup = server.getApiGroup();

        apiGroup.registerGetHandler("/hello", () -> "Hello World!");
        apiGroup.registerGetHandler("/echo", (String request) -> "Echo: " + request);

        System.out.println("ApiGroup routes: " + apiGroup);

        try {
            server.run();
        } catch (Exception e) {
            System.err.println("服务器启动失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
