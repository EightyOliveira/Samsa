package com.example.core.net;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;


public class SimpleApp implements AsyncApp {
    @Override
    public void handle(Scope scope, Receive receive, Send send) throws Exception {
        Message req = receive.receive();
        byte[] body = req.get("body");
        String text = body != null ? new String(body, StandardCharsets.UTF_8) : "";

        Map<String, Object> start = new HashMap<>();
        start.put("status", 200);
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "text/plain; charset=utf-8");
        start.put("headers", headers);
        send.send(Message.of("http.response.start", start));

        Map<String, Object> bodyMsg = new HashMap<>();
        bodyMsg.put("body", ("Echo: " + text).getBytes(StandardCharsets.UTF_8));
        bodyMsg.put("more_body", false);
        send.send(Message.of("http.response.body", bodyMsg));
    }
}

