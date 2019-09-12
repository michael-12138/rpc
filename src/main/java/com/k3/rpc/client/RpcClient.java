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

    public RpcResponse request(RpcRequest request) throws IOException;

    public String ping() throws IOException;

}
