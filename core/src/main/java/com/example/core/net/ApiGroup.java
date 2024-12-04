package com.example.core.net;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Supplier;


public class ApiGroup {

    private final ConcurrentHashMap<String, Handler> routeMap = new ConcurrentHashMap<>();


    public <T> void registerGetHandler(String path, Supplier<T> supplier) {
        routeMap.put(path, new SupplierHandler<>(supplier));
    }

    public <T, R> void registerGetHandler(String path, Function<T, R> function) {
        routeMap.put(path, new FunctionHandler<>(function));
    }


    public Optional<Handler> getHandler(String path) {
        return Optional.ofNullable(routeMap.get(path));
    }

//
//    public <T> Optional<T> invokeSupplierHandler(String path) {
//        return getHandler(path)
//                .filter(handler -> handler instanceof SupplierHandler)
//                .map(handler -> ((SupplierHandler<T>) handler).get());
//    }
//
//
//    public <T, R> Optional<R> invokeFunctionHandler(String path, T input) {
//        return getHandler(path)
//                .filter(handler -> handler instanceof FunctionHandler)
//                .map(handler -> ((FunctionHandler<T, R>) handler).apply(input));
//    }


}
