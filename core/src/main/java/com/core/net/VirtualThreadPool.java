package com.core.net;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

public class VirtualThreadPool {

    private static volatile ExecutorService INSTANCE;

    private VirtualThreadPool() {

    }

    public static ExecutorService getInstance() {
        if (INSTANCE == null) {
            synchronized (VirtualThreadPool.class) {
                if (INSTANCE == null) {
                    ThreadFactory factory = Thread.ofVirtual().factory();
                    INSTANCE = Executors.newThreadPerTaskExecutor(factory);
                }
            }
        }
        return INSTANCE;
    }


    public static void shutdown() {
        if (INSTANCE != null) {
            synchronized (VirtualThreadPool.class) {
                if (INSTANCE != null) {
                    INSTANCE.shutdown();
                    INSTANCE = null;
                }
            }
        }
    }
}
