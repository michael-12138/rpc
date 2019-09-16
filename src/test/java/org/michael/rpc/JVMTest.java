package org.michael.rpc;

import org.michael.rpc.client.RpcClient;
import org.michael.rpc.client.SimpleRpcClientPool;
import org.michael.rpc.common.Node;
import org.michael.rpc.registry.ServerDiscovery;
import org.michael.rpc.registry.ServerRegistry;
import org.junit.Test;
import org.michael.common.Configuration;
import org.michael.common.utils.IOUtil;
import org.michael.common.utils.SystemUtil;

import java.io.IOException;
import java.util.Collections;
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
        RpcClient rpcClient = new SimpleRpcClientPool(1, node, Configuration.createConfiguration(Collections.emptyMap()));
        try {
            String ping = rpcClient.ping();

            System.out.println(ping);

            String ping1 = rpcClient.ping();

            System.out.println(ping1);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (Throwable throwable) {
            throwable.printStackTrace();
        } finally {
            IOUtil.closeQuietely(rpcClient);
        }
    }

}
