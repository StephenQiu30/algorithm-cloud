package com.stephen.cloud.common.websocket.config;

import com.stephen.cloud.common.websocket.handler.TextWebSocketFrameHandler;
import com.stephen.cloud.common.websocket.manager.ChannelManager;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolConfig;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.stream.ChunkedWriteHandler;
import io.netty.handler.timeout.IdleStateHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

/**
 * Netty WebSocket 服务器配置类
 * 负责初始化 WebSocket 服务器，设置相关处理器，以及启动和关闭服务器
 * <p>
 * WebSocket协议通过Http协议升级连接为WebSocket协议，保持长连接
 *
 * @author StephenQiu30
 */
@Slf4j
@Configuration
public class NettyWebSocketServer {

    // 线程组：用于接收客户端连接请求的Boss线程组
    private EventLoopGroup bossGroup;

    // 线程组：用于处理客户端请求的Worker线程组
    private EventLoopGroup workerGroup;

    // WebSocket 服务器 Channel
    private Channel serverChannel;

    private final ChannelManager channelManager;

    public NettyWebSocketServer(ChannelManager channelManager) {
        this.channelManager = channelManager;
    }

    /**
     * 启动 WebSocket 服务器
     *
     * @param webSocketProperties 配置WebSocket的相关属性（如端口、线程数等）
     */
    @Bean(destroyMethod = "destroy")
    public NettyWebSocketServer startServer(WebSocketProperties webSocketProperties) {
        // 校验端口号是否合法
        if (webSocketProperties.getPort() > 65535 || webSocketProperties.getPort() < 0) {
            log.warn("配置的WebSocket端口[{}]无效，使用默认端口39999", webSocketProperties.getPort());
            webSocketProperties.setPort(39999);
        }

        // 初始化 Boss 和 Worker 线程组
        bossGroup = new NioEventLoopGroup(webSocketProperties.getBossThread());
        workerGroup = new NioEventLoopGroup(webSocketProperties.getWorkerThread());

        // 创建服务器启动引导对象
        ServerBootstrap serverBootstrap = new ServerBootstrap();
        serverBootstrap.group(bossGroup, workerGroup)
                // 设置服务器端通道类型
                .channel(NioServerSocketChannel.class)
                // 设置 TCP 连接的最大等待队列长度
                .option(ChannelOption.SO_BACKLOG, 128)
                // 设置 TCP 连接保持活动状态
                .childOption(ChannelOption.SO_KEEPALIVE, true)
                // 配置 bossGroup 的处理器
                .handler(new ChannelInitializer<NioServerSocketChannel>() {
                    @Override
                    protected void initChannel(NioServerSocketChannel nioServerSocketChannel) {
                        // 添加日志处理器，记录接收的连接和请求
                        nioServerSocketChannel.pipeline().addLast(new LoggingHandler(LogLevel.INFO));
                    }
                })
                // 配置 workerGroup 的处理器
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) {
                        ChannelPipeline pipeline = ch.pipeline();
                        // 添加HTTP编解码器，用于WebSocket协议的升级过程
                        pipeline.addLast(new HttpServerCodec());
                        // 添加ChunkedWriteHandler，用于大文件传输
                        pipeline.addLast(new ChunkedWriteHandler());
                        /*
                         * HttpObjectAggregator用于聚合HTTP请求的多个块，以便构建完整的HTTP请求
                         * maxContentLength设定单次聚合的最大内容长度
                         */
                        pipeline.addLast(new HttpObjectAggregator(8192));
                        /*
                         * IdleStateHandler用于检测连接的空闲状态
                         * readerIdleTime: 读超时时间，即多长时间没有读取到数据就触发读空闲事件（60秒）
                         * writerIdleTime: 写超时时间，即多长时间没有写数据就触发写空闲事件（30秒，服务器主动心跳）
                         * allIdleTime: 读写超时时间，即多长时间没有读写数据就触发读写空闲事件
                         */
                        pipeline.addLast(new IdleStateHandler(60, 30, 0, TimeUnit.SECONDS));
                        /*
                         * WebSocketServerProtocolHandler用于将HTTP协议升级为WebSocket协议，
                         * 并且支持保持WebSocket长连接。它会自动处理WebSocket握手过程
                         */
                        String path = normalizePath(webSocketProperties.getPath());
                        WebSocketServerProtocolConfig wsConfig = WebSocketServerProtocolConfig.newBuilder()
                                .websocketPath(path)
                                .checkStartsWith(true)
                                .allowExtensions(true)
                                .build();
                        pipeline.addLast(new WebSocketServerProtocolHandler(wsConfig));
                        // 添加自定义的WebSocket数据处理器，处理具体的消息逻辑
                        // 每个连接创建新的handler实例
                        pipeline.addLast(new TextWebSocketFrameHandler(channelManager));
                    }
                });

        // 在新线程中启动服务器，避免阻塞Spring容器启动
        new Thread(() -> {
            try {
                // 绑定端口，并同步等待成功
                ChannelFuture bindFuture = serverBootstrap.bind(webSocketProperties.getPort()).sync();
                serverChannel = bindFuture.channel();
                log.info("WebSocket 服务器启动成功，监听端口：{}", webSocketProperties.getPort());

                // 等待服务器关闭
                serverChannel.closeFuture().sync();
                log.info("WebSocket 服务器已关闭");
            } catch (Exception e) {
                log.error("WebSocket 服务器启动失败", e);
                if (e instanceof InterruptedException) {
                    Thread.currentThread().interrupt();
                }
                destroy();
            }
        }, "WebSocket-Server-Thread").start();

        return this;
    }

    private String normalizePath(String path) {
        if (path == null) {
            return "/websocket";
        }
        String trimmed = path.trim();
        if (trimmed.isEmpty()) {
            return "/websocket";
        }
        return trimmed.startsWith("/") ? trimmed : "/" + trimmed;
    }

    /**
     * 关闭WebSocket服务器，并优雅地释放资源
     */
    public void destroy() {
        if (serverChannel != null) {
            serverChannel.close();
        }
        if (bossGroup != null) {
            bossGroup.shutdownGracefully();
        }
        if (workerGroup != null) {
            workerGroup.shutdownGracefully();
        }
        log.info("WebSocket 服务器资源已释放");
    }
}
