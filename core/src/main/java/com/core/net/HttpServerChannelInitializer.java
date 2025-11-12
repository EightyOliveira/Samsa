package com.core.net;// HttpServerChannelInitializer.java

import com.core.router.Router;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;

public class HttpServerChannelInitializer extends ChannelInitializer<SocketChannel> {

    private final Router router;

    public HttpServerChannelInitializer(Router router) {
        this.router = router;
    }

    @Override
    protected void initChannel(SocketChannel ch) {
        ch.pipeline()
                .addLast(new HttpServerCodec())
                .addLast(new HttpObjectAggregator(65536)) // 64KB
                .addLast(new HttpServerHandler(router));
    }
}
