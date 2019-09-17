package org.michael.rpc.registry;

import org.apache.zookeeper.KeeperException;
import org.michael.rpc.common.Node;
import org.apache.commons.lang3.StringUtils;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;
import org.michael.common.Constants;
import org.michael.common.utils.StringUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Created on 2019-09-06 16:30
 * Author : Michael.
 */
public class ServerDiscovery {

    private static final Logger logger = LoggerFactory.getLogger(ServerDiscovery.class);

    private final String regAddress;
    private final String regPath;
    private final int zkSessionTimeout;
    private final AtomicReference<List<Node>> latestNodeList;
    private final List<ServerDiscoveryListener> listeners = new LinkedList<>();
    private final CountDownLatch isConnected = new CountDownLatch(1);

    public ServerDiscovery(String regAddress, String regPath) {
        this(regAddress, regPath, 10 * 1000);
    }

    public ServerDiscovery(String regAddress, String regPath, int zkSessionTimeout) {
        this.regAddress = regAddress;
        this.regPath = regPath;
        this.zkSessionTimeout = zkSessionTimeout;
        this.latestNodeList = new AtomicReference<>(null);

        try {
            ZooKeeper zk = connect();
            watchNode(zk);
        } catch (Exception e) {
            throw new RuntimeException("ServerDiscovery initialize failed.");
        }

    }

    private void onServerNodeChange() {
        synchronized (listeners) {
            for (ServerDiscoveryListener listener : listeners) {
                listener.onServerNodeChange(this);
            }
            printServerList();
        }
    }

    public void addListener(ServerDiscoveryListener listener) {
        if (listener == null) {
            return;
        }
        synchronized (listeners) {
            listeners.add(listener);
        }
    }

    public List<Node> nodeList() {
        return latestNodeList.get();
    }

    private ZooKeeper connect() throws Exception {
        ZooKeeper zk = null;
        try {
            zk = new ZooKeeper(regAddress, zkSessionTimeout, new Watcher() {
                @Override
                public void process(WatchedEvent event) {
                    if (event.getState() == Event.KeeperState.SyncConnected) {
                        isConnected.countDown();
                    }
                }
            });
            isConnected.await();
        } catch (Exception e) {
            logger.error("Connect zookeeper [ {} ] failed.", regAddress);
            throw e;
        }
        return zk;
    }

    private void watchNode(final ZooKeeper zk) throws Exception {
        try {
            List<String> children = zk.getChildren(regPath, new Watcher() {
                @Override
                public void process(WatchedEvent event) {
                    if (event.getType() == Event.EventType.NodeChildrenChanged) {
                        try {
                            watchNode(zk);
                        } catch (Exception e) {
                            logger.error("Watch zookeeper node [ " + regPath + " ] failed.", e);
                        }
                    }
                }
            });

            List<Node> nodeList = getNodes(zk, children);
            latestNodeList.set(nodeList);
            onServerNodeChange();
        } catch (Exception e) {
            logger.error("Get zookeeper children [ " + regPath + " ] failed.");
            throw e;
        }
    }

    private List<Node> getNodes(ZooKeeper zk, List<String> children) throws KeeperException, InterruptedException {
        List<Node> nodeList = new ArrayList<>(4);
        for (String node : children) {
            byte[] bytes = zk.getData(regPath + "/" + node, false, null);
            String s = new String(bytes, Constants.UTF8);
            String[] kv = StringUtils.split(s, ":");
            if (kv == null || kv.length != 2) {
                continue;
            }
            String host = kv[0];
            Integer port = StringUtil.toInt(kv[1]);
            if (StringUtils.isBlank(host) || port == null) {
                continue;
            }
            host = host.trim();
            nodeList.add(new Node(host, port));
        }
        return nodeList;
    }

    private void printServerList() {
        StringBuilder builder = new StringBuilder();
        for (Node node : latestNodeList.get()) {
            builder.append(node.toString());
            builder.append(",");
        }
        if (builder.length() > 0) {
            builder.setLength(builder.length() - 1);
        }
        logger.info("Latest available node list [ {} ]", builder);
    }
}
