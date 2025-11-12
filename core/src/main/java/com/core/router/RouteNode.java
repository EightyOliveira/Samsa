package com.core.router;

import com.core.middle.HandlerWithMiddleware;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class RouteNode {
    private final String part;

    private final Map<String, RouteNode> children = new ConcurrentHashMap<>();

    private RouteNode paramChild; // 用于 /user/{id} 这类

    private volatile HandlerWithMiddleware wrappedHandler;

    public RouteNode(String part) {
        this.part = part;
    }

    public void insert(String[] parts, int index, HandlerWithMiddleware wrappedHandler) {
        if (index == parts.length) {
            if (this.wrappedHandler != null) {
                throw new IllegalArgumentException("Duplicate route");
            }
            this.wrappedHandler = wrappedHandler;
            return;
        }

        String part = parts[index];
        boolean isParamPart = part.startsWith("{") && part.endsWith("}");

        if (isParamPart) {
            if (paramChild == null) {
                paramChild = new RouteNode(part);
            }
            paramChild.insert(parts, index + 1, wrappedHandler);
        } else {
            children.computeIfAbsent(part, RouteNode::new)
                    .insert(parts, index + 1, wrappedHandler);
        }
    }

    public MatchResult match(String[] parts, int index, Map<String, String> params) {
        if (index == parts.length) {
            if (wrappedHandler != null) {
                return new MatchResult(wrappedHandler, new HashMap<>(params));
            }
            return null;
        }

        String part = parts[index];

        // 静态匹配
        RouteNode staticChild = children.get(part);
        if (staticChild != null) {
            MatchResult result = staticChild.match(parts, index + 1, params);
            if (result != null) return result;
        }

        // 参数匹配
        if (paramChild != null) {
            String paramName = paramChild.part.substring(1, paramChild.part.length() - 1);
            params.put(paramName, part);
            MatchResult result = paramChild.match(parts, index + 1, params);
            params.remove(paramName);
            return result;
        }

        return null;
    }

    public record MatchResult(HandlerWithMiddleware wrappedHandler, Map<String, String> pathParams) {
    }
}
