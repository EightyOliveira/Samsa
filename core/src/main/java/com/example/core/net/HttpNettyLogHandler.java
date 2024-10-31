package com.example.core.net;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.FullHttpRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicInteger;


@ChannelHandler.Sharable
public class HttpNettyLogHandler extends ChannelInboundHandlerAdapter {

    static AtomicInteger successCount = new AtomicInteger();
    private static final Logger log = LoggerFactory.getLogger(HttpNettyLogHandler.class);

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        FullHttpRequest httpRequest = (FullHttpRequest) msg;
        String url = httpRequest.uri();
        log.info("url:{}", url);
        log.info("success request count:{}", successCount);
        //forward the message to the next handler in the pipeline
        ctx.fireChannelRead(msg);
    }
}
