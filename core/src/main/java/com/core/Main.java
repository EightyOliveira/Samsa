package com.core;

import com.core.middle.Middleware;
import com.core.net.WebServer;

public class Main {
    public static void main(String[] args) throws Exception {
        var app = new WebServer();


        Middleware logger = (ctx, next) -> {
            System.out.println("→ " + ctx.method() + " " + ctx.uri());
            long start = System.nanoTime();
            Object res = next.handle();
            long ms = (System.nanoTime() - start) / 1_000_000;
            System.out.println("← " + ms + "ms");
            return res;
        };
        app.use(logger);

        app.get("/hello", ctx -> "Hello World!");

        app.get("/user/{id}", ctx -> {
            String id = ctx.pathParam("id");
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return "User ID: " + id;
        });

        app.start(8080);
    }
}
