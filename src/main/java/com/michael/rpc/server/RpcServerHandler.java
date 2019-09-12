package com.michael.rpc.server;

import com.michael.rpc.common.RpcRequest;
import com.michael.rpc.common.RpcResponse;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.timeout.ReadTimeoutException;
import io.netty.handler.timeout.WriteTimeoutException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.util.Map;

/**
 * Created on 2019-09-09 19:35
 * Author : Michael.
 */
public class RpcServerHandler extends SimpleChannelInboundHandler<RpcRequest> {

    private static final Logger logger = LoggerFactory.getLogger(RpcServerHandler.class);

    private final Map<String, Object> serviceBeans;

    public RpcServerHandler(Map<String, Object> serviceBeans) {
        this.serviceBeans = serviceBeans;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, RpcRequest request) throws Exception {
        RpcResponse response = new RpcResponse();
        response.setRequestId(request.getRequestId());
        try {
            Object result = handle(request);
            response.setResult(result);
        } catch (Throwable t) {
            response.setError(t);
        }
        ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE_ON_FAILURE);
    }

    private Object handle(RpcRequest request) throws Throwable {
        String className = request.getClassName();
        String methodName = request.getMethodName();
        Class<?>[] parameterTypes = request.getParameterTypes();
        Object[] parameters = request.getParameters();

        Object serviceBean = serviceBeans.get(className);
        Class<?> forName = Class.forName(className);
        Method method = forName.getMethod(methodName, parameterTypes);
        return method.invoke(serviceBean, parameters);
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
    }

    private String clientIp(final ChannelHandlerContext ctx) {
        InetSocketAddress insocket = (InetSocketAddress) ctx.channel().remoteAddress();
        String clientIP = insocket.getAddress().getHostAddress();
        return "cip=" + clientIP;
    }
}
