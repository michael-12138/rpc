package com.michael.rpc.common;

import java.util.Objects;

/**
 * Created on 2019-09-10 10:10
 * Author : Michael.
 */
public class Node {

    public final String host;
    public final int port;

    public Node(String host, int port) {
        this.host = host;
        this.port = port;
    }

    @Override
    public String toString() {
        return "Node{" + host + ':' + port + '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        Node node = (Node) o;
        return port == node.port && Objects.equals(host, node.host);
    }

    @Override
    public int hashCode() {
        return Objects.hash(host, port);
    }
}
