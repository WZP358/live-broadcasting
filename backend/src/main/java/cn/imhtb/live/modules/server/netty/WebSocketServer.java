package cn.imhtb.live.modules.server.netty;

import cn.imhtb.live.modules.server.netty.handler.HttpHeaderHandler;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.cors.CorsConfig;
import io.netty.handler.codec.http.cors.CorsConfigBuilder;
import io.netty.handler.codec.http.cors.CorsHandler;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.stream.ChunkedWriteHandler;
import io.netty.handler.timeout.IdleStateHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.management.ManagementFactory;
import java.nio.charset.Charset;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * netty实现直播间聊天
 * @author pinteh
 * @date 2023/6/4
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "pulselive.websocket.enabled", havingValue = "true", matchIfMissing = true)
public class WebSocketServer {

    public static final int INET_PORT = 10022;
    private final EventLoopGroup bossGroup = new NioEventLoopGroup(1);
    private final EventLoopGroup workerGroup = new NioEventLoopGroup(8);

    @PostConstruct
    public void run(){
        try {
            cleanupPortBeforeBind(INET_PORT);
            startServer();
            log.info("websocket server start success, port : {}", INET_PORT);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("websocket server start error", e);
        }
    }

    private void cleanupPortBeforeBind(int port) {
        if (!System.getProperty("os.name", "").toLowerCase().contains("win")) {
            return;
        }

        String currentPid = getCurrentPid();
        Set<String> pids = findListeningPids(port);
        pids.remove(currentPid);
        for (String pid : pids) {
            if (isPulseLiveProcess(pid)) {
                killProcess(pid, port);
            } else {
                log.warn("skip websocket port cleanup for non-project process, port={}, pid={}", port, pid);
            }
        }
    }

    private String getCurrentPid() {
        String name = ManagementFactory.getRuntimeMXBean().getName();
        int separator = name.indexOf('@');
        return separator > 0 ? name.substring(0, separator) : "";
    }

    private boolean isPulseLiveProcess(String pid) {
        String commandLine = getProcessCommandLine(pid).toLowerCase();
        String userDir = System.getProperty("user.dir", "").toLowerCase();
        return commandLine.contains("cn.imhtb.live.pulseliveapplication")
                || commandLine.contains("cn.imhtb.live.antliveapplication")
                || (userDir.contains("live") && commandLine.contains(userDir));
    }

    private String getProcessCommandLine(String pid) {
        String command = "wmic process where processid=" + pid + " get CommandLine /value";
        StringBuilder output = new StringBuilder();
        try {
            Process process = new ProcessBuilder("cmd.exe", "/c", command)
                    .redirectErrorStream(true)
                    .start();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                    process.getInputStream(), Charset.defaultCharset()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append('\n');
                }
            }
            process.waitFor(5, TimeUnit.SECONDS);
        } catch (IOException exception) {
            log.warn("websocket server process command line query failed, pid={}", pid, exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            log.warn("websocket server process command line query interrupted, pid={}", pid, exception);
        }
        return output.toString();
    }

    private Set<String> findListeningPids(int port) {
        Set<String> pids = new LinkedHashSet<>();
        String command = "netstat -ano -p tcp | findstr /R /C:\":" + port + " .*LISTENING\"";
        try {
            Process process = new ProcessBuilder("cmd.exe", "/c", command)
                    .redirectErrorStream(true)
                    .start();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                    process.getInputStream(), Charset.defaultCharset()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    String[] parts = line.trim().split("\\s+");
                    if (parts.length > 0) {
                        pids.add(parts[parts.length - 1]);
                    }
                }
            }
            process.waitFor(5, TimeUnit.SECONDS);
        } catch (IOException exception) {
            log.warn("websocket server port scan failed, port={}", port, exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            log.warn("websocket server port scan interrupted, port={}", port, exception);
        }
        return pids;
    }

    private void killProcess(String pid, int port) {
        try {
            Process process = new ProcessBuilder("cmd.exe", "/c", "taskkill", "/PID", pid, "/F")
                    .redirectErrorStream(true)
                    .start();
            boolean exited = process.waitFor(10, TimeUnit.SECONDS);
            if (exited && process.exitValue() == 0) {
                log.info("cleaned stale websocket server process before bind, port={}, pid={}", port, pid);
            } else {
                log.warn("failed to cleanup stale websocket server process before bind, port={}, pid={}, exited={}",
                        port, pid, exited);
            }
        } catch (IOException exception) {
            log.warn("websocket server process cleanup failed, port={}, pid={}", port, pid, exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            log.warn("websocket server process cleanup interrupted, port={}, pid={}", port, pid, exception);
        }
    }

    private void startServer() throws InterruptedException {
        ServerBootstrap bootstrap = new ServerBootstrap();
        bootstrap.group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
                .handler(new LoggingHandler(LogLevel.INFO))
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel socketChannel) throws Exception {
                        ChannelPipeline pipeline = socketChannel.pipeline();
                        // 配置心跳检测处理器
                        pipeline.addLast(new IdleStateHandler(30, 0,0));
                        pipeline.addLast(new HttpServerCodec());
                        pipeline.addLast(new ChunkedWriteHandler());
                        pipeline.addLast(new HttpObjectAggregator(1024 * 64));
                        // 添加 CORS 处理
                        CorsConfig corsConfig = CorsConfigBuilder.forAnyOrigin()
                                .allowNullOrigin()
                                .allowCredentials()
                                .build();
                        pipeline.addLast(new CorsHandler(corsConfig));
                        // 自定义头部处理器
                        pipeline.addLast(new HttpHeaderHandler());
                        // webSocket处理器
                        pipeline.addLast(new WebSocketServerProtocolHandler("/"));
                        pipeline.addLast(new WebSocketServerHandler());
                    }
                });
        // 启动服务
        bootstrap.bind(INET_PORT).sync();
    }

}
