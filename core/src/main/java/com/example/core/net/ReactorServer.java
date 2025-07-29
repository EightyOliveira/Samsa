package com.example.core.net;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ReactorServer {
    private static final Logger logger = LoggerFactory.getLogger(ReactorServer.class);
    private final int port;
    private final ApiGroup apiGroup;
    private final ExecutorService virtualThreadPool;

    public ReactorServer(int port) {
        this.port = port;
        this.apiGroup = new ApiGroup();
        this.virtualThreadPool = Executors.newVirtualThreadPerTaskExecutor();
    }

    public void run() {
        try (Selector selector = Selector.open();
             ServerSocketChannel serverSocket = ServerSocketChannel.open()) {

            serverSocket.bind(new InetSocketAddress(port));
            serverSocket.configureBlocking(false);
            serverSocket.register(selector, SelectionKey.OP_ACCEPT);

            logger.info("Server started on port {}", port);

            while (true) {
                selector.select();
                Iterator<SelectionKey> keys = selector.selectedKeys().iterator();

                while (keys.hasNext()) {
                    SelectionKey key = keys.next();
                    keys.remove();

                    if (key.isAcceptable()) {
                        acceptConnection(selector, serverSocket);
                    } else if (key.isReadable()) {
                        virtualThreadPool.submit(() -> handleRequest(key));
                    }
                }
            }
        } catch (IOException e) {
            logger.error("Server error", e);
        }
    }

    private void acceptConnection(Selector selector, ServerSocketChannel serverSocket) throws IOException {
        SocketChannel clientChannel = serverSocket.accept();
        clientChannel.configureBlocking(false);
        clientChannel.register(selector, SelectionKey.OP_READ);
    }

    private void handleRequest(SelectionKey key) {
        try {
            SocketChannel channel = (SocketChannel) key.channel();
            ReactorHandler handler = new ReactorHandler(apiGroup, channel);
            handler.handle();

            // 如果是短连接则关闭
            if (!handler.isKeepAlive()) {
                channel.close();
            }
        } catch (IOException e) {
            try {
                key.channel().close();
                System.out.println(e.getMessage());
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }

    public ApiGroup getApiGroup() {
        return apiGroup;
    }
}
