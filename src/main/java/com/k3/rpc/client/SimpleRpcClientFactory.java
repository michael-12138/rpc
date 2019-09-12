package com.k3.rpc.client;

import com.k3.rpc.common.Node;
import org.k3.common.Configuration;
import org.k3.common.ObjectPool;
import org.k3.common.utils.IOUtil;

import java.io.IOException;

/**
 * Created on 2019-09-12 15:50
 * Author : Michael.
 */
public class SimpleRpcClientFactory implements ObjectPool.ObjectFactory<SimpleRpcClient> {

    private final Node node;
    private final Configuration conf;
    private final int socketTimeout;
    private final int connectTimeout;

    public SimpleRpcClientFactory(Node node, Configuration conf) {
        this.node = node;
        this.conf = conf;
        this.socketTimeout = conf.getInt("rpc.client.socket.timeout", 5000);
        this.connectTimeout = conf.getInt("rpc.client.connect.timeout", 5000);
    }

    @Override
    public boolean isValid(SimpleRpcClient o) {
        if (!o.needCheckValid(System.currentTimeMillis())) {
            return true;
        }
        try {
            String r = o.ping();
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    @Override
    public SimpleRpcClient createObject() throws Exception {
        SimpleRpcClient client = new SimpleRpcClient(node, conf, socketTimeout);
        boolean err = false;
        try {
            client.connect(connectTimeout);
            return client;
        } catch (IOException e) {
            err = true;
            throw e;
        } finally {
            if (err) {
                IOUtil.closeQuietely(client);
            }
        }
    }

    @Override
    public void releaseObject(SimpleRpcClient e) {
        IOUtil.closeQuietely(e);
    }
}
