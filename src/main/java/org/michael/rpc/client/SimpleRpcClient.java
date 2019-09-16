package org.michael.rpc.client;

import org.michael.rpc.common.*;
import org.michael.rpc.service.BasicService;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import org.michael.common.Configuration;
import org.michael.common.utils.IOUtil;
import org.michael.common.utils.ShortUuid;
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

    private long lastCheckValidTime;

    public SimpleRpcClient(Node node, Configuration conf, int socketTimeout) {
        this.node = node;
        this.socketTimeout = socketTimeout;
        this.clientHandler = new RpcClientHandler();
        this.worker = new NioEventLoopGroup();
        this.bootstrap = new Bootstrap();
        this.lastCheckValidTime = System.currentTimeMillis();
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
//                    ch.pipeline().addLast("read-timeout", new ReadTimeoutHandler(socketTimeout, TimeUnit.MILLISECONDS));
//                    ch.pipeline().addLast("write-timeout", new WriteTimeoutHandler(socketTimeout, TimeUnit.MILLISECONDS));
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
    public RpcResponse request(RpcRequest request) throws IOException {
        try {
            channelFuture.channel().writeAndFlush(request).sync();
            return clientHandler.waitResponse(socketTimeout);
        } catch (Exception e) {
            throw new IOException(String.format("Request failed: %s, %s.", node.toString(), request.toString()), e);
        }
    }

    @Override
    public String ping() throws IOException {
        RpcRequest request = new RpcRequest();
        request.setRequestId(ShortUuid.newShortUuid());
        request.setClassName(BasicService.class.getName());
        request.setMethodName("ping");
        request.setParameterTypes(null);
        request.setParameters(null);

        RpcResponse response = this.request(request);
        if (response.isError()) {
            throw new IOException(response.getError());
        } else {
            return String.valueOf(response.getResult());
        }
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

    public boolean needCheckValid(long now) {
        long gap = now - lastCheckValidTime;
        if (gap < 5000) {
            return false;
        }
        lastCheckValidTime = now;
        return true;
    }
}
