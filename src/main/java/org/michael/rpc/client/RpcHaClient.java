package org.michael.rpc.client;

import org.michael.common.Configuration;
import org.michael.common.utils.IOUtil;
import org.michael.rpc.common.Node;
import org.michael.rpc.common.RpcRequest;
import org.michael.rpc.common.RpcResponse;
import org.michael.rpc.registry.ServerDiscovery;
import org.michael.rpc.registry.ServerDiscoveryListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Created on 2019-09-12 16:53
 * Author : Michael.
 */
public class RpcHaClient implements RpcClient {

    private static final Logger logger = LoggerFactory.getLogger(RpcHaClient.class);

    private final Configuration conf;
    private final int coreSizePerNode;
    private final Lock lock;
    private final ServerDiscovery serverDiscovery;
    private volatile Node[] nodes;
    private final Map<Node, RpcClient> node2client;
    private final int requestRetry;

    public RpcHaClient(Configuration conf) {
        this.conf = conf;
        this.coreSizePerNode = conf.getInt("rpc.client.pool.core.size.per.node", 4);
        this.lock = new ReentrantLock();
        String regAddr = conf.getString("rpc.registery.zookeeper.address");
        String regPath = conf.getString("rpc.registery.zookeeper.path", "/rpc");
        int zkSessionTimeout = conf.getInt("rpc.registry.zookeeper.session.timeout.ms", 10 * 1000);
        this.serverDiscovery = new ServerDiscovery(regAddr, regPath, zkSessionTimeout);
        List<Node> nodeList = serverDiscovery.nodeList();
        if (nodeList.isEmpty()) {
            throw new RuntimeException("No available server nodes.");
        }
        this.nodes = nodeList.toArray(new Node[nodeList.size()]);
        this.node2client = initClients(nodeList);
        this.serverDiscovery.addListener(new ServerDiscoveryListener() {
            @Override
            public void onServerNodeChange(ServerDiscovery serverDiscovery) {
                List<Node> nodes = serverDiscovery.nodeList();
                updateNode2Client(nodes);
            }
        });
        this.requestRetry = conf.getInt("rpc.client.ha.request.retry", 3);
    }

    @Override
    public RpcResponse request(RpcRequest request) throws IOException {
        int retry = this.requestRetry;
        IOException ex = null;
        while (retry > 0) {
            RpcClient client = pickNode();
            try {
                return client.request(request);
            } catch (IOException e) {
                ex = e;
                retry--;
            }
        }
        throw new IOException(String.format("Request failed on %s retries.", this.requestRetry), ex);
    }

    @Override
    public String ping() throws IOException {
        int retry = this.requestRetry;
        IOException ex = null;
        while (retry > 0) {
            RpcClient client = pickNode();
            try {
                return client.ping();
            } catch (IOException e) {
                ex = e;
                retry--;
            }
        }
        throw new IOException(String.format("Request failed on %s retries.", this.requestRetry), ex);
    }

    private final AtomicInteger pickIndex = new AtomicInteger(0);

    private RpcClient pickNode() {
        lock.lock();
        try {
            int index = pickIndex.incrementAndGet();
            if (index > 100000) {
                pickIndex.set(0);
            }
            index = index % this.nodes.length;
            Node node = nodes[index];
            return node2client.get(node);
        } finally {
            lock.unlock();
        }
    }

    private void updateNode2Client(List<Node> latestNodes) {
        lock.lock();
        try {
            this.nodes = latestNodes.toArray(new Node[latestNodes.size()]);
            for (Node node : latestNodes) {
                if (!node2client.containsKey(node)) {
                    node2client.put(node, initClient(node));
                    logger.info("Server {} join.", node.toString());
                }
            }
            for (Node node : node2client.keySet()) {
                if (!latestNodes.contains(node)) {
                    RpcClient client = node2client.remove(node);
                    logger.info("Server {} disconnect, close it.", node.toString());
                    IOUtil.closeQuietely(client);
                }
            }
        } finally {
            lock.unlock();
        }
    }

    private Map<Node, RpcClient> initClients(List<Node> nodes) {
        Map<Node, RpcClient> map = new ConcurrentHashMap<>();
        for (Node node : nodes) {
            map.put(node, initClient(node));
        }
        return map;
    }

    private RpcClient initClient(Node node) {
        RpcClient client = new SimpleRpcClientPool(coreSizePerNode, node, conf);
        return client;
    }

    @Override
    public void close() throws IOException {
        for (RpcClient client : this.node2client.values()) {
            IOUtil.closeQuietely(client);
        }
    }
}
