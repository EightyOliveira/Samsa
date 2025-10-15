package com.example.core.net;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public final class Message {
    private final String type;
    private final Map<String, Object> data;

    public Message(String type, Map<String, Object> data) {
        this.type = type;
        this.data = data != null ? Collections.unmodifiableMap(new HashMap<>(data)) : Collections.emptyMap();
    }

    public String type() { return type; }

    @SuppressWarnings("unchecked")
    public <T> T get(String key) { return (T) data.get(key); }

    public static Message of(String type) { return new Message(type, null); }
    public static Message of(String type, Map<String, Object> data) { return new Message(type, data); }
}
