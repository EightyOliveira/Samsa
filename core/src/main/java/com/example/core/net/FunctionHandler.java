package com.example.core.net;

import java.util.function.Function;

public class FunctionHandler<T, R> implements Handler {
    private final Function<T, R> function;

    public FunctionHandler(Function<T, R> function) {
        this.function = function;
    }

    public R apply(T t) {
        return function.apply(t);
    }
}