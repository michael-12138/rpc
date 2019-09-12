package com.michael.rpc.registry;

import org.apache.zookeeper.*;
import org.k3.common.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CountDownLatch;

/**
 * Created on 2019-09-06 14:22
 * Author : Michael.
 */
public class ServerRegistry {

    private static final Logger logger = LoggerFactory.getLogger(ServerRegistry.class);

    private final String regAddress;
    private final String regPath;
    private final int zkSessionTimeout;
    private final CountDownLatch isConnected = new CountDownLatch(1);

    public ServerRegistry(String regAddress, String regPath) {
        this(regAddress, regPath, 10 * 1000);
    }

    public ServerRegistry(String regAddress, String regPath, int zkSessionTimeout) {
        this.regAddress = regAddress;
        this.regPath = regPath;
        this.zkSessionTimeout = zkSessionTimeout;
    }

    public boolean register(String data) {
        boolean err = false;
        ZooKeeper zk = null;
        try {
            zk = connect();
            createNode(zk, data);
        } catch (Exception e) {
            err = true;
            logger.error("Service register failed.", e);
        } finally {
            if (err == true && zk != null) {
                try {
                    zk.close();
                } catch (InterruptedException e) {
                }
            }
        }
        return err == false;
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

    private void createNode(ZooKeeper zk, String data) throws Exception {
        byte[] bytes = data.getBytes(Constants.UTF8);
        try {
            if (zk.exists(regPath, false) == null) {
                zk.create(regPath, null, ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
                logger.info("Create zookeeper node [ {} -> {} ] success.", regPath, null);
            }
            String childPath = regPath + "/server";
            String finalPath = zk.create(childPath, bytes, ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL_SEQUENTIAL);
            logger.info("Register zookeeper node [ {} -> {} ] success.", finalPath, data);
        } catch (Exception e) {
            logger.error("Register zookeeper node failed.");
            throw e;
        }
    }
}
