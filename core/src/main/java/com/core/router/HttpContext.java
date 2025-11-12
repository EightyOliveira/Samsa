package com.core.router;

import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.*;
import io.netty.util.CharsetUtil;

import java.util.Map;

public class HttpContext {
    private final String method;
    private final String uri;
    private final HttpHeaders headers;
    private final Map<String, String> pathParams;
    private final ChannelHandlerContext nettyCtx;

    public HttpContext(String method, String uri, HttpHeaders headers, Map<String, String> pathParams, ChannelHandlerContext nettyCtx) {
        this.method = method;
        this.uri = uri;
        this.headers = headers;
        this.pathParams = pathParams;
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
