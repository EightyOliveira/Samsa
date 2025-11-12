package com.core.router;

@FunctionalInterface
public interface HttpHandler {

    Object handle(HttpContext ctx) throws Exception;
}
