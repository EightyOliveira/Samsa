package com.example.core.net;

@FunctionalInterface
public interface Receive {
    Message receive() throws InterruptedException;
}

