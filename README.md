# RPC

A rpc common component project. You can easily implement service publishing by simply adding the annotation @RpcService to the interface implementation class.

### Dependency and maven install

        mvn clean source:jar install -DskipTests=true

        <dependency>
            <groupId>org.michael.rpc</groupId>
            <artifactId>rpc</artifactId>
            <version>1.0.0</version>
        </dependency>

### Server deploying

Usually requires specify parameters: 'rpc.netty.socket.bind.add', 'rpc.netty.socket.bind.port'. If deploy a highly available service, you need to specify 'rpc.registery.zookeeper.address'. Otherwise the client cannot discover the server.


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

### Service publishing

```java
//create service interface
public interface HelloService {

    String hello1(String name);

    Map<String, String> hello2(Map<String, String> map);
    
}

//create interface implement class.
//adding the annotation @RpcService to the interface implementation class.
@RpcService(HelloService.class)
public class HelloServiceImpl implements HelloService {
    @Override
    public String hello1(String name) {
        System.out.println("Remote calling method: hello1, param name: " + name);
        String resp = "resp: " + name;
        return resp;
    }

    @Override
    public Map<String, String> hello2(Map<String, String> map) {
        System.out.println("Remote calling method: hello2, param map: " + map.toString());
        Map<String, String> respMap = new HashMap<>(map);
        respMap.put("respKey", "respValue");
        return respMap;
    }
}

```

### Server startup

java -cp $CLASSPATH org.michael.rpc.server.RpcServer -c [conf file] -i [instance name]

  
### Client APIs

```java

//


```










