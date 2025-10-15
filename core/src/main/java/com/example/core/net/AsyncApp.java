package com.example.core.net;

@FunctionalInterface
public interface AsyncApp {
    void handle(Scope scope, Receive receive, Send send) throws Exception;
}

