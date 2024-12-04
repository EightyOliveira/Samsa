package com.example.core.net;

import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.*;

import java.nio.charset.StandardCharsets;
import java.util.Optional;


public class HttpNettyHandler extends ChannelInboundHandlerAdapter {

    private final ApiGroup apiGroup;

    public HttpNettyHandler(ApiGroup apiGroup) {
        this.apiGroup = apiGroup;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        if (msg instanceof FullHttpRequest httpRequest) {
            String uri = httpRequest.uri();
            Optional<Handler> handlerOpt = apiGroup.getHandler(uri);
            if (handlerOpt.isPresent()) {
                Handler handler = handlerOpt.get();
                sendResponse(ctx, httpRequest, handler);
            } else {
                sendErrorResponse(ctx, HttpResponseStatus.NOT_FOUND);
            }
        } else {
            ctx.fireChannelRead(msg);
        }
    }

    private void sendResponse(ChannelHandlerContext ctx, FullHttpRequest request, Handler handler) {
        if (handler instanceof SupplierHandler<?> supplierHandler) {
            Object responseContent = supplierHandler.get();
            sendResponseWithContentType(ctx, request, responseContent);
        } else if (handler instanceof FunctionHandler) {
            FunctionHandler<FullHttpRequest, ?> functionHandler = (FunctionHandler<FullHttpRequest, ?>) handler;
            Object responseContent = functionHandler.apply(request);
            sendResponseWithContentType(ctx, request, responseContent);
        } else {
            sendErrorResponse(ctx, HttpResponseStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private void sendResponseWithContentType(ChannelHandlerContext ctx, FullHttpRequest request, Object responseContent) {
        String contentType;
        byte[] contentBytes;

        if (responseContent instanceof String) {
            contentType = "text/plain; charset=UTF-8";
            contentBytes = ((String) responseContent).getBytes(StandardCharsets.UTF_8);
        } else if (responseContent instanceof byte[]) {
            contentType = "application/octet-stream";
            contentBytes = (byte[]) responseContent;
        }
//        else if (responseContent instanceof byte[]) {
//            contentType = "application/json; charset=UTF-8"; // 如果是 JSON 数据
//            contentBytes = (byte[]) responseContent;
//        }
        else {
            contentType = "text/plain; charset=UTF-8";
            contentBytes = responseContent.toString().getBytes(StandardCharsets.UTF_8);
        }

        DefaultFullHttpResponse response = new DefaultFullHttpResponse(
                HttpVersion.HTTP_1_1,
                HttpResponseStatus.OK,
                Unpooled.wrappedBuffer(contentBytes)
        );
        response.headers().set(HttpHeaderNames.CONTENT_TYPE, contentType);
        response.headers().set(HttpHeaderNames.CONTENT_LENGTH, response.content().readableBytes());

        // 如果是 HTTP/1.1 并且不是 Keep-Alive，则关闭连接
        if (!HttpUtil.isKeepAlive(request)) {
            ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
        } else {
            response.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE);
            ctx.writeAndFlush(response);
        }
    }

    private void sendErrorResponse(ChannelHandlerContext ctx, HttpResponseStatus status) {
        byte[] contentBytes = ("Error: " + status.reasonPhrase()).getBytes(StandardCharsets.UTF_8);
        DefaultFullHttpResponse response = new DefaultFullHttpResponse(
                HttpVersion.HTTP_1_1,
                status,
                Unpooled.wrappedBuffer(contentBytes)
        );
        response.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/plain; charset=UTF-8");
        response.headers().set(HttpHeaderNames.CONTENT_LENGTH, response.content().readableBytes());

        ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
    }
}