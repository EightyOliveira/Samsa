package com.example.core.net;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.netty.http.server.HttpServer;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ReactorServer {
    private static final Logger logger = LoggerFactory.getLogger(ReactorServer.class);
    private final int port;
    private final ApiGroup apiGroup;
    private final ExecutorService virtualThreadPool;

    public ReactorServer(int port) {
        this.port = port;
        this.apiGroup = new ApiGroup();
        this.virtualThreadPool = Executors.newVirtualThreadPerTaskExecutor();
    }

    public void run() {
        logger.info("Starting server on port {}", port);

        HttpServer.create()
                .port(port)
                .handle((req, res) -> {
                    String method = req.method().name();
                    String path = req.uri().split("\\?")[0];
                    Optional<Handler> handlerOpt = apiGroup.getHandler(method, path);

                    if (handlerOpt.isEmpty()) {
                        res.status(404);
                        return res.header("Content-Type", "text/plain")
                                .sendString(Mono.just("Not Found"))
                                .then();
                    }

                    Handler handler = handlerOpt.get();

                    if (handler instanceof SupplierHandler<?> supplierHandler) {
                        return Mono.fromCallable(supplierHandler::get)
                                .subscribeOn(Schedulers.fromExecutor(virtualThreadPool))
                                .map(Object::toString)
                                .flatMap(content -> res.header("Content-Type", "text/plain")
                                        .sendString(Mono.just(content))
                                        .then());
                    }

                    if (handler instanceof FunctionHandler) {
                        return req.receive()
                                .aggregate()
                                .asString(StandardCharsets.UTF_8)
                                .flatMap(body -> Mono.fromCallable(() -> ((FunctionHandler<String, ?>) handler).apply(body))
                                        .subscribeOn(Schedulers.fromExecutor(virtualThreadPool))
                                        .map(Object::toString))
                                .flatMap(content -> res.header("Content-Type", "text/plain")
                                        .sendString(Mono.just(content))
                                        .then());
                    }

                    res.status(500);
                    return res.header("Content-Type", "text/plain")
                            .sendString(Mono.just("Unsupported handler type"))
                            .then();
                })
                .bindNow(Duration.ofSeconds(30))
                .onDispose()
                .block();
    }

    public ApiGroup getApiGroup() {
        return apiGroup;
    }
}
