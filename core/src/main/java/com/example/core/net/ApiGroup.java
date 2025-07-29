package com.example.core.net;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Supplier;


public class ApiGroup {

    private final ConcurrentHashMap<String, Handler> getMap = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Handler> postMap = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Handler> putMap = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Handler> deleteMap = new ConcurrentHashMap<>();

    public <T> void registerGetHandler(String path, Supplier<T> supplier) {
        getMap.put(path, new SupplierHandler<>(supplier));
    }

    public <T, R> void registerGetHandler(String path, Function<T, R> function) {
        getMap.put(path, new FunctionHandler<>(function));
    }

    public <T, R> void registerPostHandler(String path, Function<T, R> function) {
        postMap.put(path, new FunctionHandler<>(function));
    }

    public <T> void registerPutHandler(String path, Function<T, Void> function) {
        putMap.put(path, new FunctionHandler<>(function));
    }

    public void registerDeleteHandler(String path, Runnable action) {
        deleteMap.put(path, new SupplierHandler<>(() -> {
            action.run();
            return null;
        }));
    }

    public Optional<Handler> getHandler(String method, String path) {
        return switch (method) {
            case "GET" -> Optional.ofNullable(getMap.get(path));
            case "POST" -> Optional.ofNullable(postMap.get(path));
            case "PUT" -> Optional.ofNullable(putMap.get(path));
            case "DELETE" -> Optional.ofNullable(deleteMap.get(path));
            default -> Optional.empty();
        };
    }

    @Override
    public String toString() {
        return "ApiGroup{" +
                "GET=" + getMap.keySet() + ", " +
                "POST=" + postMap.keySet() + ", " +
                "PUT=" + putMap.keySet() + ", " +
                "DELETE=" + deleteMap.keySet() + "}";
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
