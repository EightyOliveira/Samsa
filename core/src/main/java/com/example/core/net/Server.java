package com.example.core.net;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Minimal uvicorn-like server using Java 21 virtual threads to simulate asyncio.
 */
public record Server(AsyncApp app, int port) {
    private final static Logger logger = LoggerFactory.getLogger(Server.class);

    public void start() {
        try (ServerSocket server = new ServerSocket(port)) {
            logger.info("Server listening on {}", port);
            while (!server.isClosed()) {
                Socket socket = server.accept();
                Thread.startVirtualThread(() -> handleConnection(socket));
            }
        } catch (IOException ignored) {
            logger.info("Server closed");
        }
    }

    private void handleConnection(Socket socket) {
        try (socket; InputStream in = socket.getInputStream(); OutputStream out = socket.getOutputStream()) {
            HttpRequest req = parseHttp(in);
            LinkedBlockingQueue<Message> recvQ = new LinkedBlockingQueue<>();
            LinkedBlockingQueue<Message> sendQ = new LinkedBlockingQueue<>();

            Map<String, Object> reqMeta = new HashMap<>();

            Scope scope = new Scope("http", req.method, req.path, req.headers);

            Thread.startVirtualThread(() -> {
                try {
                    app.handle(scope, recvQ::take, sendQ::put);
                } catch (Throwable t) {
                    try {
                        Map<String, Object> start = new HashMap<>();
                        start.put("status", 500);
                        start.put("headers", Map.of("Content-Type", "text/plain; charset=utf-8"));
                        sendQ.put(Message.of("http.response.start", start));
                        sendQ.put(Message.of("http.response.body", Map.of("body", ("Internal error: " + t.getMessage()).getBytes(StandardCharsets.UTF_8), "more_body", false)));
                    } catch (InterruptedException ignored) {
                        logger.warn("Interrupted while waiting for response");
                    }
                }
            });

            try {
                int remaining = req.contentLength;
                if (remaining <= 0) {
                    reqMeta.put("body", null);
                    reqMeta.put("more_body", false);
                    recvQ.put(Message.of("http.request", reqMeta));
                } else {
                    byte[] buffer = new byte[8192];
                    while (remaining > 0) {
                        int toRead = Math.min(buffer.length, remaining);
                        int n = in.read(buffer, 0, toRead);
                        if (n == -1) {
                            recvQ.put(Message.of("http.request", Map.of("body", new byte[0], "more_body", false)));
                            break;
                        }
                        byte[] chunk = new byte[n];
                        System.arraycopy(buffer, 0, chunk, 0, n);
                        boolean more = (remaining - n) > 0;
                        Map<String, Object> data = new HashMap<>();
                        data.put("body", chunk);
                        data.put("more_body", more);
                        recvQ.put(Message.of("http.request", data));
                        remaining -= n;
                    }
                }
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
            }

            boolean headersWritten = false;
            while (true) {
                Message m = sendQ.take();
                if ("http.response.start".equals(m.type())) {
                    int status = m.get("status");
                    Map<String, String> headers = m.get("headers");
                    writeStatusAndHeaders(out, status, headers);
                    headersWritten = true;
                } else if ("http.response.body".equals(m.type())) {
                    byte[] body = m.get("body");
                    boolean more = Boolean.TRUE.equals(m.get("more_body"));
                    if (!headersWritten) {
                        writeStatusAndHeaders(out, 200, Map.of("Content-Type", "application/octet-stream"));
                        headersWritten = true;
                    }
                    if (body != null) out.write(body);
                    if (!more) {
                        out.flush();
                        break;
                    }
                }
            }

        } catch (Exception e) {
            logger.info("connection error: {}", String.valueOf(e));
        }
    }

    private void writeStatusAndHeaders(OutputStream out, int status, Map<String, String> headers) throws IOException {
        String statusLine = "HTTP/1.1 " + status + " " + statusText(status) + "\r\n";
        out.write(statusLine.getBytes(StandardCharsets.US_ASCII));
        if (headers != null) {
            for (Map.Entry<String, String> h : headers.entrySet()) {
                out.write((h.getKey() + ": " + h.getValue() + "\r\n").getBytes(StandardCharsets.US_ASCII));
            }
        }
        out.write("\r\n".getBytes(StandardCharsets.US_ASCII));
    }

    private static String statusText(int status) {
        return switch (status) {
            case 200 -> "OK";
            case 400 -> "Bad Request";
            case 404 -> "Not Found";
            case 500 -> "Internal Server Error";
            default -> "";
        };
    }

    private static final class HttpRequest {
        final String method;
        final String path;
        final Map<String, String> headers;
        final int contentLength;

        HttpRequest(String method, String path, Map<String, String> headers, int contentLength) {
            this.method = method;
            this.path = path;
            this.headers = headers;
            this.contentLength = contentLength;
        }
    }

    private HttpRequest parseHttp(InputStream in) throws IOException {
        ByteArrayOutputStream headerBuf = new ByteArrayOutputStream();
        int state = 0;
        while (true) {
            int b = in.read();
            if (b == -1) break;
            headerBuf.write(b);
            if (state == 0 && b == '\r') state = 1;
            else if (state == 1 && b == '\n') state = 2;
            else if (state == 2 && b == '\r') state = 3;
            else if (state == 3 && b == '\n') {
                break;
            } else if (b == '\r') state = 1;
            else state = 0;
        }

        String headerText = headerBuf.toString(StandardCharsets.US_ASCII);
        String[] lines = headerText.split("\\r\\n");
        if (lines.length == 0) throw new IOException("empty request");
        String requestLine = lines[0];
        String[] parts = requestLine.split(" ");
        String method = parts[0];
        String path = parts.length > 1 ? parts[1] : "/";

        Map<String, String> headers = new HashMap<>();
        for (int i = 1; i < lines.length; i++) {
            String line = lines[i];
            if (line.isEmpty()) continue;
            int idx = line.indexOf(':');
            if (idx > 0) {
                String name = line.substring(0, idx).trim();
                String value = line.substring(idx + 1).trim();
                headers.put(name, value);
            }
        }

        int contentLength = 0;
        if (headers.containsKey("Content-Length")) {
            try {
                contentLength = Integer.parseInt(headers.get("Content-Length"));
            } catch (NumberFormatException ignored) {
            }
        }

        return new HttpRequest(method, path, headers, contentLength);
    }

    public static void main(String[] args) throws Exception {
        int port = 8000;
        Server server = new Server(new SimpleApp(), port);
        server.start();
    }
}
