package com.example.core.net;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.util.CharsetUtil;
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
        String body = httpRequest.content().toString(CharsetUtil.UTF_8);
        log.info("Request body is: {}", body);
        String uri = httpRequest.uri();
        log.info("Request URL is: {}", uri);
        log.info("success request count:{}", successCount.incrementAndGet());
        //forward the message to the next handler in the pipeline
        ctx.fireChannelRead(msg);
    }
}
