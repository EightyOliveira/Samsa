package com.core.router;

import com.core.middle.HandlerWithMiddleware;
import com.core.middle.Middleware;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Router {
    private final Map<String, RouteNode> trees = new ConcurrentHashMap<>();


    public void addRoute(String method, String path, List<Middleware> middlewares, HttpHandler handler) {
        String normalized = path.startsWith("/") ? path : "/" + path;
        String[] parts = normalized.split("/");
        RouteNode root = trees.computeIfAbsent(method, k -> new RouteNode("/"));
        HandlerWithMiddleware wrapped = new HandlerWithMiddleware(handler, middlewares);
        root.insert(parts, 0, wrapped);
    }



    public RouteNode.MatchResult find(String method, String uri) {
        RouteNode root = trees.get(method);
        if (root == null) return null;
        String path = uri.split("\\?")[0];
        String[] parts = path.split("/");
        return root.match(parts, 0, new HashMap<>());
    }
}
