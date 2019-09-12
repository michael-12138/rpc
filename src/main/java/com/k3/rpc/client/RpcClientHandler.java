package com.k3.rpc.client;

import com.k3.rpc.common.RpcResponse;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    private RpcResponse response;

    public RpcClientHandler() {
        this.lock = new ReentrantLock();
        this.arrived = lock.newCondition();
    }

    public RpcResponse getResponse() {
        return response;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, RpcResponse response) throws Exception {
        lock.lock();
        try {
            this.response = response;
            arrived.signalAll();
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        logger.error("Client caught exception, closed.", cause);
        ctx.close();
    }
}
