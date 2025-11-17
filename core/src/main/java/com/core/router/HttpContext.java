package com.core.router;

import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.*;
import io.netty.util.CharsetUtil;

import java.util.Map;
import java.io.IOException;
import com.core.net.MessagePackMapper;

public class HttpContext {
    private final String method;
    private final String uri;
    private final HttpHeaders headers;
    private final Map<String, String> pathParams;
    private final ChannelHandlerContext nettyCtx;
    private final byte[] body;

    public HttpContext(String method, String uri, HttpHeaders headers, Map<String, String> pathParams, byte[] body, ChannelHandlerContext nettyCtx) {
        this.method = method;
        this.uri = uri;
        this.headers = headers;
        this.pathParams = pathParams;
        this.body = body;
        this.nettyCtx = nettyCtx;
    }

    public String pathParam(String name) {
        return pathParams.get(name);
    }

    public String method() {
        return method;
    }

    public String uri() {
        return uri;
    }

    public HttpHeaders headers() {
        return headers;
    }


    public void text(String body) {
        writeResponse(200, "text/plain; charset=UTF-8", body);
    }


    public void msgpack(Object obj) throws IOException {
        byte[] bytes = MessagePackMapper.toBytes(obj);
        FullHttpResponse response = new DefaultFullHttpResponse(
                HttpVersion.HTTP_1_1,
                HttpResponseStatus.OK,
                Unpooled.wrappedBuffer(bytes)
        );
        response.headers().set(HttpHeaderNames.CONTENT_TYPE, "application/msgpack");
        response.headers().set(HttpHeaderNames.CONTENT_LENGTH, response.content().readableBytes());
        nettyCtx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
    }


    public <T> T bodyAs(Class<T> clazz) throws IOException {
        if (body == null || body.length == 0) return null;
        return MessagePackMapper.fromBytes(body, clazz);
    }

    private void writeResponse(int status, String contentType, String body) {
        FullHttpResponse response = new DefaultFullHttpResponse(
                HttpVersion.HTTP_1_1,
                HttpResponseStatus.valueOf(status),
                Unpooled.copiedBuffer(body, CharsetUtil.UTF_8)
        );
        response.headers().set(HttpHeaderNames.CONTENT_TYPE, contentType);
        response.headers().set(HttpHeaderNames.CONTENT_LENGTH, response.content().readableBytes());
        nettyCtx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
    }
}
