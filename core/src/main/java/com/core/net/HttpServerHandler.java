package com.core.net;

import com.core.middle.Middleware;
import com.core.router.HttpContext;
import com.core.router.HttpHandler;
import com.core.router.Router;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.*;
import io.netty.util.CharsetUtil;

import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

public class HttpServerHandler extends SimpleChannelInboundHandler<FullHttpRequest> {

    private final Router router;
    private final List<Middleware> globalMiddlewares = new CopyOnWriteArrayList<>();

    public final static Logger log = Logger.getLogger(HttpServerHandler.class.getName());


    public HttpServerHandler(Router router) {
        this.router = router;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest req) {
        // 使用虚拟线程池异步处理请求，同时维护活跃请求计数
        VirtualThreadPool.getInstance().execute(() -> {
            ActivateRequest.incrementActive();
            try {
                String uri = req.uri();
                String method = req.method().name();

                var match = router.find(method, uri);
                if (match == null) {
                    send404(ctx);
                    return;
                }

                HttpContext httpCtx = new HttpContext(method, uri, req.headers(), match.pathParams(), ctx);

                // 执行中间件链并获取结果
                Object result = match.wrappedHandler().execute(httpCtx);

                String body = result == null ? "" : (result instanceof String s ? s : result.toString());
                sendResponse(ctx, 200, body);
            } catch (Exception e) {
                log.log(Level.WARNING, e.getMessage(), e);
                sendResponse(ctx, 500, "Internal Error");
            } finally {
                ActivateRequest.decrementActive();
            }
        });
    }

    private void send404(ChannelHandlerContext ctx) {
        sendResponse(ctx, 404, "Not Found");
    }

    private void sendResponse(ChannelHandlerContext ctx, int status, String body) {
        FullHttpResponse response = new DefaultFullHttpResponse(
                HttpVersion.HTTP_1_1,
                HttpResponseStatus.valueOf(status),
                Unpooled.copiedBuffer(body, CharsetUtil.UTF_8)
        );
        response.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/plain; charset=UTF-8");
        response.headers().set(HttpHeaderNames.CONTENT_LENGTH, response.content().readableBytes());
        ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        log.log(Level.WARNING, cause.getMessage(), cause);
        ctx.close();
    }

    // 全局中间件
    public void use(Middleware middleware) {
        globalMiddlewares.add(middleware);
    }

    // 路由方法（带中间件）
    public void get(String path, List<Middleware> middlewares, HttpHandler handler) {
        router.addRoute("GET", path, combineMiddlewares(middlewares), handler);
    }

    public void get(String path, HttpHandler handler) {
        get(path, List.of(), handler);
    }

    // POST 等类似...

    // 合并全局 + 路由级中间件
    private List<Middleware> combineMiddlewares(List<Middleware> routeMiddlewares) {
        List<Middleware> all = new ArrayList<>(globalMiddlewares);
        all.addAll(routeMiddlewares);
        return all;
    }
}
