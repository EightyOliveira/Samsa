package com.example.core.net;

@FunctionalInterface
public interface Send {
    void send(Message message) throws InterruptedException;
}

