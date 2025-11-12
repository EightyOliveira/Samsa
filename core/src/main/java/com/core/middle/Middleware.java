package com.core.middle;

import com.core.router.HttpContext;

@FunctionalInterface
public interface Middleware {
    Object handle(HttpContext ctx, HandlerChain next) throws Exception;
}
