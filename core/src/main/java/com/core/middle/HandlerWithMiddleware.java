package com.core.middle;

import com.core.router.HttpContext;
import com.core.router.HttpHandler;

import java.util.List;

public class HandlerWithMiddleware {
    private final HttpHandler finalHandler;
    private final List<Middleware> middlewares;

    public HandlerWithMiddleware(HttpHandler finalHandler, List<Middleware> middlewares) {
        this.finalHandler = finalHandler;
        this.middlewares = List.copyOf(middlewares);
    }


    public Object execute(HttpContext ctx) throws Exception {
        HandlerChain chain = buildChain(0, ctx);
        return chain.handle();
    }

    private HandlerChain buildChain(int index, HttpContext ctx) {
        if (index == middlewares.size()) {
            // 所有中间件执行完，调用最终 handler
            return () -> finalHandler.handle(ctx);
        }

        Middleware mw = middlewares.get(index);
        HandlerChain next = buildChain(index + 1, ctx);
        return () -> mw.handle(ctx, next);
    }
}
