package com.core.middle;

@FunctionalInterface
public interface HandlerChain {
    Object handle() throws Exception;
}
