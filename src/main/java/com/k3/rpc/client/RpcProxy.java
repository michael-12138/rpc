package com.k3.rpc.client;

import com.k3.rpc.common.Node;
import com.k3.rpc.common.RpcRequest;
import com.k3.rpc.common.RpcResponse;
import org.k3.common.Configuration;
import org.k3.common.utils.IOUtil;
import org.k3.common.utils.ShortUuid;

import java.io.Closeable;
import java.io.IOException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/**
 * Created on 2019-09-09 20:23
 * Author : Michael.
 */
public class RpcProxy {

    private final RpcClient rpcClient;

    public RpcProxy(RpcClient rpcClient) {
        this.rpcClient = rpcClient;
//        int coreSize = conf.getInt("rpc.client.pool.core.size.per.node", 4);
//        this.rpcClient = new SimpleRpcClientPool(coreSize, node, conf);
    }

    public <T> T create(Class<T> interfaceClass) {
        Class<?>[] interfaces = new Class[]{interfaceClass};
        T instance = (T) Proxy.newProxyInstance(interfaceClass.getClassLoader(), interfaces, new InvocationHandler() {
            @Override
            public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                RpcRequest request = new RpcRequest();
                request.setRequestId(ShortUuid.newShortUuid());
                request.setClassName(method.getDeclaringClass().getName());
                request.setMethodName(method.getName());
                request.setParameterTypes(method.getParameterTypes());
                request.setParameters(args);

                RpcResponse response = RpcProxy.this.rpcClient.request(request);
                if (response.isError()) {
                    throw response.getError();
                } else {
                    return response.getResult();
                }
            }
        });
        return instance;
    }
}
