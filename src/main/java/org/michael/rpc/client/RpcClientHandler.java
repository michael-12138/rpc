package org.michael.rpc.client;

import org.michael.rpc.common.RpcResponse;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.timeout.ReadTimeoutException;
import io.netty.handler.timeout.WriteTimeoutException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Created on 2019-09-10 10:57
 * Author : Michael.
 */
public class RpcClientHandler extends SimpleChannelInboundHandler<RpcResponse> {

    private static final Logger logger = LoggerFactory.getLogger(RpcClientHandler.class);

    protected final Lock lock;
    protected final Condition arrived;
    private final AtomicReference<RpcResponse> response;

    public RpcClientHandler() {
        this.lock = new ReentrantLock();
        this.arrived = lock.newCondition();
        this.response = new AtomicReference<>(null);
    }

    public RpcResponse getResponse() {
        return this.response.get();
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, RpcResponse response) throws Exception {
        handle(response);
    }

    private void handle(RpcResponse response) {
        lock.lock();
        try {
            this.response.set(response);
            arrived.signalAll();
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        if (cause instanceof ReadTimeoutException) {
            String msg = clientIp(ctx);
            logger.info("{} read timeout, closed.", msg);
            ctx.close();
        } else if (cause instanceof WriteTimeoutException) {
            String msg = clientIp(ctx);
            logger.info("{} write timeout, closed.", msg);
            ctx.close();
        } else {
            super.exceptionCaught(ctx, cause);
        }
        RpcResponse response = new RpcResponse();
        response.setError(cause);
        handle(response);
    }

    private String clientIp(final ChannelHandlerContext ctx) {
        InetSocketAddress insocket = (InetSocketAddress) ctx.channel().remoteAddress();
        String serverIP = insocket.getAddress().getHostAddress();
        return "sip=" + serverIP;
    }
}
