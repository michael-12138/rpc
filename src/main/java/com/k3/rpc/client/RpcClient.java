package com.k3.rpc.client;

import com.k3.rpc.common.RpcRequest;
import com.k3.rpc.common.RpcResponse;

import java.io.Closeable;
import java.io.IOException;

/**
 * Created on 2019-09-10 10:07
 * Author : Michael.
 */
public interface RpcClient extends Closeable {

    public RpcProxy getRpcProxy();

    public RpcResponse request(RpcRequest request) throws Throwable;

    public String ping() throws Throwable;

}
