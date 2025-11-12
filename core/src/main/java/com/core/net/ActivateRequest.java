package com.core.net;

import java.util.concurrent.atomic.AtomicInteger;

public class ActivateRequest {
    private static final AtomicInteger activeRequests = new AtomicInteger(0);

    public static void incrementActive() {
        activeRequests.incrementAndGet();
    }

    public static void decrementActive() {
        activeRequests.decrementAndGet();
    }

    public static int getActiveRequests() {
        return activeRequests.get();
    }
}
