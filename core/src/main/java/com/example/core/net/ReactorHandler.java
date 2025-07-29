package com.example.core.net;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

public class ReactorHandler {
    private static final Logger logger = LoggerFactory.getLogger(ReactorHandler.class);
    private final ApiGroup apiGroup;
    private final SocketChannel channel;
    private final ByteBuffer readBuffer = ByteBuffer.allocate(8192);
    private boolean keepAlive = false;

    public ReactorHandler(ApiGroup apiGroup, SocketChannel channel) {
        this.apiGroup = apiGroup;
        this.channel = channel;
    }

    public void handle() throws IOException {
        // 1. 读取HTTP请求
        String httpRequest = readHttpRequest();

        // 2. 解析请求方法和路径
        String method = parseMethod(httpRequest);
        String path = parsePath(httpRequest);

        // 3. 查找对应的Handler
        Optional<Handler> handlerOpt = apiGroup.getHandler(method, path);

        logger.debug("Handling {} {}", method, path);

        // 4. 处理请求并发送响应
        if (handlerOpt.isPresent()) {
            Handler handler = handlerOpt.get();
            String response = processHandler(handler, httpRequest);
            sendResponse(response);
        } else {
            logger.warn("Route not found: {} {}", method, path);
            sendErrorResponse(404, "Not Found");
        }
    }

    private String readHttpRequest() throws IOException {
        readBuffer.clear();
        StringBuilder requestBuilder = new StringBuilder();
        int bytesRead;
        long startTime = System.currentTimeMillis();

        // 设置读取超时5秒
        while ((bytesRead = channel.read(readBuffer)) > 0 ||
                (System.currentTimeMillis() - startTime < 5000 && bytesRead == 0)) {
            if (bytesRead > 0) {
                readBuffer.flip();
                requestBuilder.append(StandardCharsets.UTF_8.decode(readBuffer));
                readBuffer.clear();
                // 检查是否已经读取到完整的HTTP请求(以空行结束)
                if (requestBuilder.toString().contains("\r\n\r\n")) {
                    break;
                }
            }
        }

        if (bytesRead == -1) {
            throw new IOException("Connection closed by client");
        }

        return requestBuilder.toString();
    }

    private String parseMethod(String httpRequest) {
        String[] lines = httpRequest.split("\r\n");
        if (lines.length > 0) {
            String[] parts = lines[0].split(" ");
            if (parts.length > 0) {
                return parts[0];
            }
        }
        return "GET";
    }

    private String parsePath(String httpRequest) {
        // 解析HTTP请求的第一行获取路径
        String[] lines = httpRequest.split("\r\n");
        if (lines.length > 0) {
            String[] parts = lines[0].split(" ");
            if (parts.length > 1) {
                // 检查Connection头
                for (String line : lines) {
                    if (line.startsWith("Connection:")) {
                        keepAlive = line.contains("keep-alive");
                        break;
                    }
                }
                // 获取原始路径并去除查询参数
                String fullPath = parts[1];
                return fullPath.split("\\?")[0];
            }
        }
        return "/";
    }

    public boolean isKeepAlive() {
        return keepAlive;
    }

    private String processHandler(Handler handler, String httpRequest) {
        try {
            if (handler instanceof SupplierHandler<?> supplierHandler) {
                Object response = supplierHandler.get();
                return buildResponse(200, "OK", response.toString());
            } else if (handler instanceof FunctionHandler) {
                FunctionHandler<String, ?> functionHandler = (FunctionHandler<String, ?>) handler;
                // 提取请求体
                String requestBody = extractRequestBody(httpRequest);
                Object response = functionHandler.apply(requestBody);
                return buildResponse(200, "OK", response.toString());
            }
            logger.error("Unsupported handler type");
            return buildResponse(500, "Internal Server Error", "Unsupported handler type");
        } catch (Exception e) {
            logger.error("Handler processing error", e);
            return buildResponse(500, "Internal Server Error", e.getMessage());
        }
    }

    private String extractRequestBody(String httpRequest) {
        String[] parts = httpRequest.split("\r\n\r\n", 2);
        return parts.length > 1 ? parts[1] : "";
    }

    private String buildResponse(int statusCode, String statusText, String content) {
        return "HTTP/1.1 " + statusCode + " " + statusText + "\r\n" +
                "Content-Type: text/plain\r\n" +
                "Content-Length: " + content.length() + "\r\n" +
                "\r\n" +
                content;
    }

    private void sendResponse(String response) throws IOException {
        channel.write(ByteBuffer.wrap(response.getBytes(StandardCharsets.UTF_8)));
    }

    private void sendErrorResponse(int statusCode, String message) throws IOException {
        String response = buildResponse(statusCode, message, message);
        sendResponse(response);
    }
}
