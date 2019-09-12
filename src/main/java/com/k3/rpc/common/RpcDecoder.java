package com.k3.rpc.common;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;

import java.util.List;

/**
 * Created on 2019-09-09 17:39
 * Author : Michael.
 */
public class RpcDecoder extends ByteToMessageDecoder {

    private final Class<?> clazz;

    public RpcDecoder(Class<?> clazz) {
        this.clazz = clazz;
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        if (in.readableBytes() < 4) {
            return;
        }
        int size = in.readInt();
        byte[] buff = new byte[size];
        in.readBytes(buff);
        in.clear();
        Object obj = SerializationUtil.deserialize(buff, clazz);
        out.add(obj);
    }
}
