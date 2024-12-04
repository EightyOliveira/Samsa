package com.example.core.net;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpContentCompressor;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.codec.http.HttpResponseEncoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class HttpNettyServer {

    private final Integer port;

    private static final Logger log = LoggerFactory.getLogger(HttpNettyServer.class);

    private final ApiGroup apiGroup;

    public HttpNettyServer(int port) {
        this.port = port;
        apiGroup = new ApiGroup();
    }


    public HttpNettyServer() {
        this(NettyServerConstant.PORT);
    }


    public void run() {
        ExecutorService virtualThreadPool = Executors.newVirtualThreadPerTaskExecutor();

        EventLoopGroup bossGroup = new NioEventLoopGroup(NettyServerConstant.Max_Master_Thread, virtualThreadPool);

        EventLoopGroup workerGroup = new NioEventLoopGroup(NettyServerConstant.Max_Worker_Thread, virtualThreadPool);

        final HttpNettyLogHandler httpNettyLogHandler = new HttpNettyLogHandler();
        try {
            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .localAddress(port)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel socketChannel) {
                            socketChannel.pipeline()
                                    .addLast("encoder", new HttpResponseEncoder())
                                    .addLast("decoder", new HttpRequestDecoder())
                                    .addLast("aggregator", new HttpObjectAggregator(10 * 1024 * 1024))
                                    .addLast("compressor", new HttpContentCompressor())
                                    .addLast(httpNettyLogHandler)
                                    .addLast(new HttpNettyHandler(apiGroup));
                        }
                    });
            ChannelFuture future = bootstrap.bind().sync();
            future.channel().closeFuture().sync();
        } catch (Exception e) {
            log.error("server start failed:{}", e.getMessage());
        } finally {
            try {
                bossGroup.shutdownGracefully().sync();
                workerGroup.shutdownGracefully().sync();
            } catch (InterruptedException e) {
                log.error("server is stop:{}", e.getMessage());
            }
        }
    }


    public ApiGroup getApiGroup() {
        return apiGroup;
    }
}