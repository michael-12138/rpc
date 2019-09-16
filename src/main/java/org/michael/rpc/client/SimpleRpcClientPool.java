package org.michael.rpc.client;

import org.michael.rpc.common.Node;
import org.michael.rpc.common.RpcRequest;
import org.michael.rpc.common.RpcResponse;
import org.michael.common.Configuration;
import org.michael.common.ObjectPool;

import java.io.IOException;

/**
 * Created on 2019-09-12 13:52
 * Author : Michael.
 */
public class SimpleRpcClientPool extends ObjectPool<SimpleRpcClient> implements RpcClient {

    public SimpleRpcClientPool(int coreSize, Node node, Configuration conf) {
        super(coreSize, new SimpleRpcClientFactory(node, conf));
    }

    @Override
    public RpcResponse request(RpcRequest request) throws IOException {
        boolean err = false;
        SimpleRpcClient client = null;
        try {
            client = this.acquire();
            return client.request(request);
        } catch (Exception e) {
            err = true;
            throw new IOException(e);
        } finally {
            this.release(client, err);
        }
    }

    @Override
    public String ping() throws IOException {
        boolean err = false;
        SimpleRpcClient client = null;
        try {
            client = this.acquire();
            return client.ping();
        } catch (Exception e) {
            err = true;
            throw new IOException(e);
        } finally {
            this.release(client, err);
        }
    }

    @Override
    public void close() throws IOException {
        this.releaseAllObject();
    }
}
