package com.k3.rpc.common;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

/**
 * Created on 2019-09-09 19:29
 * Author : Michael.
 */
public class RpcEncoder extends MessageToByteEncoder<Object> {

    private Class<?> clazz;

    public RpcEncoder(Class<?> clazz) {
        this.clazz = clazz;
    }

    @Override
    protected void encode(ChannelHandlerContext ctx, Object inob, ByteBuf out) throws Exception {
        if (clazz.isInstance(inob)) {
            byte[] bytes = SerializationUtil.serialize(inob);
            out.writeInt(bytes.length);
            out.writeBytes(bytes);
        }
    }
}
