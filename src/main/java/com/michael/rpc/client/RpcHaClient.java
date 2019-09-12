package com.michael.rpc.client;

import com.michael.rpc.common.RpcRequest;
import com.michael.rpc.common.RpcResponse;

import java.io.IOException;

/**
 * Created on 2019-09-12 16:53
 * Author : Michael.
 */
public class RpcHaClient implements RpcClient {


    @Override
    public RpcResponse request(RpcRequest request) throws IOException {
        return null;
    }

    @Override
    public String ping() throws IOException {
        return null;
    }

    @Override
    public void close() throws IOException {

    }
}
