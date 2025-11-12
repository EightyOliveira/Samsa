package com.core.net;

import com.core.router.HttpHandler;
import com.core.router.Router;
import com.core.middle.Middleware;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;

public class WebServer {

    private final Router router = new Router();
    private final List<Middleware> globalMiddlewares = new CopyOnWriteArrayList<>();
    public static final Logger log = Logger.getLogger(WebServer.class.getName());


    private volatile Channel serverChannel;
    private volatile EventLoopGroup bossGroup;
    private volatile EventLoopGroup workerGroup;

    private final AtomicBoolean started = new AtomicBoolean(false);
    private final AtomicBoolean stopping = new AtomicBoolean(false);


    public WebServer() {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                stop(Duration.ofSeconds(10));
            } catch (Exception e) {
                log.severe("Error during shutdown: " + e.getMessage());
            }
        }));
    }


    public void get(String path, HttpHandler handler) {
        router.addRoute("GET", path, combineMiddlewares(List.of()), handler);
    }

    public void post(String path, HttpHandler handler) {
        router.addRoute("POST", path, combineMiddlewares(List.of()), handler);
    }

    public void delete(String path, HttpHandler handler) {
        router.addRoute("DELETE", path, combineMiddlewares(List.of()), handler);
    }

    public void use(Middleware m) {
        globalMiddlewares.add(m);
    }

    private List<Middleware> combineMiddlewares(List<Middleware> routeMiddlewares) {
        List<Middleware> all = new ArrayList<>(globalMiddlewares);
        all.addAll(routeMiddlewares);
        return all;
    }


    public void start(int port) throws InterruptedException {
        if (!started.compareAndSet(false, true)) {
            throw new IllegalStateException("Server is already started");
        }

        bossGroup = new NioEventLoopGroup(1);
        workerGroup = new NioEventLoopGroup();

        try {
            ServerBootstrap b = new ServerBootstrap();
            b.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new HttpServerChannelInitializer(router));

            ChannelFuture f = b.bind(port).sync();
            this.serverChannel = f.channel();

            log.info("Server started on http://localhost:" + port);
            f.channel().closeFuture().sync();
        } finally {

            if (started.get()) {
                stop(Duration.ZERO);
            }
        }
    }

    public void stop() {
        try {
            stop(Duration.ofSeconds(10));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warning("Shutdown interrupted");
        }
    }


    public void stop(Duration timeout) throws InterruptedException {
        if (!stopping.compareAndSet(false, true)) {
            log.info("Shutdown already in progress");
            return;
        }

        log.info("Shutting down server...");

        if (serverChannel != null) {
            serverChannel.close().syncUninterruptibly();
            log.info("Server socket closed.");
        }


        if (timeout.toMillis() > 0 && ActivateRequest.getActiveRequests() > 0) {
            log.info("Waiting for active requests to complete...");
            long start = System.currentTimeMillis();
            while (ActivateRequest.getActiveRequests() > 0) {
                if (System.currentTimeMillis() - start > timeout.toMillis()) {
                    log.severe("Timeout waiting for active requests to finish");
                    break;
                }
                Thread.sleep(50);
            }
        }


        if (workerGroup != null) {
            workerGroup.shutdownGracefully().syncUninterruptibly();
        }
        if (bossGroup != null) {
            bossGroup.shutdownGracefully().syncUninterruptibly();
        }


        VirtualThreadPool.shutdown();

        log.info("Server shutdown complete.");
    }
}
