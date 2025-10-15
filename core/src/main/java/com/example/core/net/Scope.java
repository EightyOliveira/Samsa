package com.example.core.net;

import java.util.Map;

public final class Scope {
    public final String type; // "http"
    public final String method;
    public final String path;
    public final Map<String, String> headers;

    public Scope(String type, String method, String path, Map<String, String> headers) {
        this.type = type;
        this.method = method;
        this.path = path;
        this.headers = headers;
    }
}

