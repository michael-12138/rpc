# RPC

A remote procedure call common component project. You can easily implement service publishing by simply adding the annotation @RpcService to the interface implementation class.

### Dependency and maven install

        mvn clean source:jar install -DskipTests=true

        <dependency>
            <groupId>org.michael.rpc</groupId>
            <artifactId>rpc</artifactId>
            <version>1.0.0</version>
        </dependency>

### Server deploying

Usually requires specify parameters: 'rpc.netty.socket.bind.add', 'rpc.netty.socket.bind.port'. If deploy a highly available service, you need to specify 'rpc.registery.zookeeper.address'. Otherwise the HA client cannot discover the server.


### Server configuration parameters and default values

rpc.registery.zookeeper.address=null

rpc.registery.zookeeper.path=/rpc

rpc.registry.zookeeper.session.timeout.ms=10 * 1000

rpc.netty.socket.bind.add=0.0.0.0

rpc.netty.socket.bind.port=12138

rpc.netty.boss.threads=4

rpc.netty.worker.threads=16

rpc.netty.so.backlog=512

rpc.netty.socket.recv.buffer.size=8192

rpc.netty.socket.send.buffer.size=8192

rpc.netty.socket.timeout=10 * 1000

### Server APIs

```java
//Create service interface.
public interface HelloService {

    String hello1(String name);

    Map<String, String> hello2(Map<String, String> map);
    
}

//Create interface implement class.
//Adding the annotation @RpcService to the interface implementation class.
@RpcService(HelloService.class)
public class HelloServiceImpl implements HelloService {
    @Override
    public String hello1(String name) {
        System.out.println("Remote calling method: hello1, param name: " + name);
        String resp = "hello1 " + name;
        return resp;
    }

    @Override
    public Map<String, String> hello2(Map<String, String> map) {
        System.out.println("Remote calling method: hello2, param map: " + map.toString());
        Map<String, String> respMap = new HashMap<>(map);
        respMap.put("hello2 ", "michael");
        return respMap;
    }
}

```

### Server startup

java -cp $CLASSPATH org.michael.rpc.server.RpcServer -c [conf file] -i [instance name]


### Client configuration parameters and default values

rpc.client.pool.core.size.per.node=4

rpc.registery.zookeeper.address   #RpcHaClient required parameter

rpc.registery.zookeeper.path=/rpc

rpc.registry.zookeeper.session.timeout.ms=10 * 1000

rpc.client.ha.request.retry=3

rpc.client.socket.timeout=5000

rpc.client.connect.timeout=5000


  
### Client APIs

```java

// Simple client API. However, we usually don't use it this way.
RpcClient client = new SimpleRpcClient(node, conf, 5000);

// Simple client pool API. It's better to use connection pooling for single-node services.
RpcClient client = new SimpleRpcClientPool(4, node, conf);

// HA Client API. We usually deploy services to multiple servers to ensure high availability of services.
// RpcHaClient is essentially a connection pool with multiple nodes, it can automatically connects to the online service and automatically shuts down the offline service.
RpcClient client = new RpcHaClient(conf);

// Finally we need to create a business interface proxy for remote method invocation.
RpcProxy proxy = new RpcProxy(client);
HelloService helloService = proxy.create(HelloService.class);
// Just like calling a local method
helloService.hello1("michael");

// Each client implements the Closeable interface, close it when it is not needed.
client.close();


```
