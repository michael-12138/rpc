package com.k3.rpc.server;

import com.k3.rpc.BeanContainer;
import com.k3.rpc.common.RpcDecoder;
import com.k3.rpc.common.RpcEncoder;
import com.k3.rpc.common.RpcRequest;
import com.k3.rpc.common.RpcResponse;
import com.k3.rpc.registry.ServerRegistry;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import org.apache.commons.cli.Options;
import org.k3.common.CommonLineParameters;
import org.k3.common.Configuration;
import org.k3.common.LifeCycle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Created on 2019-09-09 14:09
 * Author : Michael.
 */
public class RpcServer implements LifeCycle {

    private static final Logger logger = LoggerFactory.getLogger(RpcServer.class);

    private final String instance;
    private final String bindAddr;
    private final int bindPort;

    private final NioEventLoopGroup bossGroup;
    private final NioEventLoopGroup workerGroup;
    private final ServerBootstrap bootstrap;
    private ChannelFuture channelFuture;

    private ServerRegistry serverRegistry;
    private final Map<String, Object> serviceBeans;

    public RpcServer(Configuration conf, String instance) {
        this(conf, instance, null);
    }

    public RpcServer(Configuration conf, String instance, ServerRegistry serverRegistry) {
        this.instance = instance;
        this.serverRegistry = serverRegistry;
        this.serviceBeans = BeanContainer.getInstance().getAllServiceBeans();

        this.bindAddr = conf.getString("rpc.netty.socket.bind.addr", null);
        this.bindPort = conf.getInt("rpc.netty.socket.bind.port", 12138);

        int bossThreads = conf.getInt("rpc.netty.boss.threads", 4);
        int workerThreads = conf.getInt("rpc.netty.worker.threads", 16);

        int backlog = conf.getInt("rpc.netty.so.backlog", 512);
        int socketRecvBuffSize = conf.getInt("rpc.netty.socket.recv.buffer.size", 8192);
        int socketSendBuffSize = conf.getInt("rpc.netty.socket.send.buffer.size", 8192);
        int socketTimeout = conf.getInt("rpc.netty.socket.timeout", 10 * 1000);

        final int readTimeout = conf.getInt("rpc.netty.socket.read.timeout", 5 * 1000);
        final int writeTimeout = conf.getInt("rpc.netty.socket.write.timeout", 5 * 1000);

        this.bossGroup = new NioEventLoopGroup(bossThreads);
        this.workerGroup = new NioEventLoopGroup(workerThreads);
        this.bootstrap = new ServerBootstrap();
        this.bootstrap.group(bossGroup, workerGroup)
                .option(ChannelOption.SO_KEEPALIVE, false)
                .option(ChannelOption.SO_BACKLOG, backlog)
                .option(ChannelOption.TCP_NODELAY, true)
                .option(ChannelOption.SO_REUSEADDR, true)
                .option(ChannelOption.SO_RCVBUF, socketRecvBuffSize)
                .option(ChannelOption.SO_SNDBUF, socketSendBuffSize)
                .option(ChannelOption.SO_TIMEOUT, socketTimeout)
                .channel(NioServerSocketChannel.class);

        this.bootstrap.childHandler(new ChannelInitializer<SocketChannel>() {
            @Override
            protected void initChannel(SocketChannel ch) throws Exception {
                ch.pipeline().addLast("read-timeout", new ReadTimeoutHandler(readTimeout, TimeUnit.MILLISECONDS));
                ch.pipeline().addLast("write-timeout", new WriteTimeoutHandler(writeTimeout, TimeUnit.MILLISECONDS));
                ch.pipeline().addLast("rpc-decoder", new RpcDecoder(RpcRequest.class));
                ch.pipeline().addLast("rpc-encoder", new RpcEncoder(RpcResponse.class));
                ch.pipeline().addLast("rpc-handler", new RpcServerHandler(serviceBeans));
            }
        });
    }

    @Override
    public void startup() {
        String bindMsg = bindAddr == null ? "0.0.0.0:" + bindPort : bindAddr + ":" + bindPort;
        this.channelFuture = bindAddr == null ? bootstrap.bind(bindPort) : bootstrap.bind(bindAddr, bindPort);
        try {
            channelFuture.sync();
            logger.info("Server starts on " + bindMsg);
            registerServer();
        } catch (InterruptedException e) {
            logger.error("Server startup failed.");
            try {
                channelFuture.channel().close().sync();
            } catch (InterruptedException e1) {
            }
        }
    }

    private void registerServer() {
        if (serverRegistry != null) {
            String address = bindAddr;
            if (address == null) {
                try {
                    address = InetAddress.getLocalHost().getHostAddress();
                } catch (UnknownHostException e) {
                    logger.error("Get local host failed.", e);
                }
            }
            if (address != null) {
                String data = address + ":" + bindPort;
                serverRegistry.register(data);
            }
        }
    }

    @Override
    public void startFinish() throws InterruptedException {

    }

    @Override
    public void shutdown() {
        logger.info("Shutting down netty server socket...");
        try {
            this.channelFuture.channel().close().sync();
        } catch (InterruptedException e) {
            logger.error("Shutdown interrupted.", e);
        }
        logger.info("Shutting down netty thread pool...");
        this.bossGroup.shutdownGracefully();
        this.workerGroup.shutdownGracefully();
        logger.info("Showdown finished.");
    }

    @Override
    public void awaitShutdown(long timeWait, TimeUnit timeUnit) throws InterruptedException {

    }

    @Override
    public String name() {
        return "RpcServer";
    }

    private static Options initOptions() {
        Options options = new Options();
        options.addRequiredOption("c", "configFile", true, "config file");
        options.addRequiredOption("i", "instance", true, "instance name");
        return options;
    }

    private static void addShutdownHook(final RpcServer rpcServer) {
        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
            public void run() {
                logger.info("Catch shutdown signal.");
                rpcServer.shutdown();
            }
        }, "Shutdown-hook"));
    }

    public static void main(String[] args) {
        CommonLineParameters parameters = new CommonLineParameters(initOptions(), args);
        String configFile = parameters.getString("c");
        String instance = parameters.getString("i");
        Configuration conf = Configuration.fromFile(configFile);

        String regAddr = conf.getString("rpc.server.registry.addr", null);
        String regPath = conf.getString("rpc.server.registry.path", "/rpc");
        int zkSessionTimeout = conf.getInt("rpc.server.registry.zk.session.timeout.ms", 10 * 1000);
        ServerRegistry serverRegistry = null;
        if (regAddr != null) {
            serverRegistry = new ServerRegistry(regAddr, regPath, zkSessionTimeout);
        }
        RpcServer rpcServer = new RpcServer(conf, instance, serverRegistry);
        addShutdownHook(rpcServer);
        rpcServer.startup();
    }
}
