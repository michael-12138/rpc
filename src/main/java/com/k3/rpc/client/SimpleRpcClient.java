package com.k3.rpc.client;

import com.k3.rpc.common.*;
import com.k3.rpc.service.BasicService;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import org.k3.common.utils.IOUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * Created on 2019-09-10 10:56
 * Author : Michael.
 */
public class SimpleRpcClient implements RpcClient {

    private static final Logger logger = LoggerFactory.getLogger(SimpleRpcClient.class);

    private final Node node;
    private final int socketTimeout;

    private final EventLoopGroup worker;
    private final Bootstrap bootstrap;
    private ChannelFuture channelFuture;

    private final RpcClientHandler clientHandler;
    private final RpcProxy rpcProxy;

    public SimpleRpcClient(Node node, int socketTimeout) {
        this.node = node;
        this.socketTimeout = socketTimeout;
        this.clientHandler = new RpcClientHandler();
        this.worker = new NioEventLoopGroup();
        this.bootstrap = new Bootstrap();
        this.rpcProxy = new RpcProxy(this);
    }

    public void connect(int timeout) throws IOException {
        boolean err = false;
        try {
            this.bootstrap.group(worker)
                    .option(ChannelOption.SO_KEEPALIVE, false)
                    .option(ChannelOption.TCP_NODELAY, true)
                    .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, timeout)
                    .channel(NioSocketChannel.class);

            this.bootstrap.handler(new ChannelInitializer<SocketChannel>() {
                @Override
                protected void initChannel(SocketChannel ch) throws Exception {
                    ch.pipeline().addLast("rpc-encoder", new RpcEncoder(RpcRequest.class));
                    ch.pipeline().addLast("rpc-decoder", new RpcDecoder(RpcResponse.class));
                    ch.pipeline().addLast("rpc-handler", SimpleRpcClient.this.clientHandler);
                }
            });
            this.channelFuture = bootstrap.connect(node.host, node.port).sync();
        } catch (InterruptedException e) {
            err = true;
            throw new IOException(String.format("Connect to %s failed.", node.toString()), e);
        } finally {
            if (err) {
                IOUtil.closeQuietely(this);
            }
        }
    }

    @Override
    public RpcProxy getRpcProxy() {
        return this.rpcProxy;
    }

    @Override
    public RpcResponse request(RpcRequest request) throws Throwable {
        clientHandler.lock.lock();
        try {
            channelFuture.channel().writeAndFlush(request).sync();
            clientHandler.arrived.await(socketTimeout, TimeUnit.MILLISECONDS);
            return clientHandler.getResponse();
        } catch (InterruptedException e) {
            throw new IOException(String.format("Request failed: [%s]. [%s].", node.toString(), request.toString()), e);
        } finally {
            clientHandler.lock.unlock();
        }
    }

    @Override
    public String ping() throws Throwable {
        BasicService basicService = this.rpcProxy.create(BasicService.class);
        return basicService.ping();
    }

    @Override
    public void close() throws IOException {
        if (channelFuture != null) {
            try {
                channelFuture.channel().close().sync();
            } catch (InterruptedException e) {
                throw new IOException(String.format("Close the connect %s failed.", node.toString()), e);
            } finally {
                this.worker.shutdownGracefully();
            }
        } else {
            this.worker.shutdownGracefully();
        }
    }
}
