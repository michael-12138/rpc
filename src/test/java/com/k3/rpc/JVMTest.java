package com.k3.rpc;

import com.k3.rpc.client.RpcClient;
import com.k3.rpc.client.SimpleRpcClient;
import com.k3.rpc.common.Node;
import com.k3.rpc.registry.ServerDiscovery;
import com.k3.rpc.registry.ServerRegistry;
import org.junit.Test;
import org.k3.common.utils.IOUtil;
import org.k3.common.utils.SystemUtil;

import java.io.IOException;
import java.util.List;

/**
 * Created on 2019-09-06 15:17
 * Author : Sunny.
 */
public class JVMTest {

    @Test
    public void testServerRegister() {
        String registryAddress = "hadoop.slave1:2181";
        String registryPath = "/rpc";

        ServerRegistry serviceRegistry = new ServerRegistry(registryAddress, registryPath, 3000);
        boolean success = serviceRegistry.register("127.0.0.3:8081");
        System.out.println(success);
        SystemUtil.sleepQuietly(Integer.MAX_VALUE);
    }

    @Test
    public void testServerDiscovery() {
        String registryAddress = "hadoop.slave1:2181";
        String registryPath = "/rpc";

        ServerDiscovery serverDiscovery = new ServerDiscovery(registryAddress, registryPath, 3000);
        SystemUtil.sleepQuietly(Integer.MAX_VALUE);
    }

    @Test
    public void testClientPing() {
        String registryAddress = "hadoop.slave1:2181";
        String registryPath = "/rpc";

        ServerDiscovery serverDiscovery = new ServerDiscovery(registryAddress, registryPath, 3000);
        List<Node> nodes = serverDiscovery.nodeList();
        Node node = nodes.get(0);
        SimpleRpcClient rpcClient = new SimpleRpcClient(node, 5000);
        try {
            rpcClient.connect(5000);
            String ping = rpcClient.ping();

            System.out.println(ping);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (Throwable throwable) {
            throwable.printStackTrace();
        } finally {
            IOUtil.closeQuietely(rpcClient);
        }
    }

}
