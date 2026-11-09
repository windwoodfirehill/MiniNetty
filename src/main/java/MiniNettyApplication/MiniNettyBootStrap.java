package MiniNettyApplication;

import Core.BossLoop;
import Core.ChannelDispatcher;
import Core.WorkerLoop;
import Handler.MessageHandler;
import Protocol.Protocol;
import Protocol.HttpProtocol;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.*;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public class MiniNettyBootStrap {
    private static final Map<String, MessageHandler> handlerMap = new HashMap<>();
    private final int defaultPort;
    private final String defaultHost;
    private final Protocol defaultProtocol;
    private  Map<String, Supplier<Protocol>> protocolHandlers;
    public MiniNettyBootStrap(int defaultPort,String defaultHost,Protocol defaultProtocol){
        this.defaultPort = defaultPort;
        this.defaultHost = defaultHost;
        this.defaultProtocol = defaultProtocol;
    }
    public void setHandler(String protocolType,MessageHandler messageHandler){
        handlerMap.put(protocolType,messageHandler);
    }

    public void MiniteyStart() throws IOException {
        String[] protocols = {"http"};
        protocolHandlers = Map.of("protocols",HttpProtocol :: new);

        ChannelDispatcher channelDispatcher = new ChannelDispatcher();
        //原本：new InetSocketAddress()，没有无参构造器

        BossLoop bossLoop = new BossLoop(new InetSocketAddress(defaultHost, defaultPort),channelDispatcher); //修改

        WorkerLoop workerLoop = new WorkerLoop(defaultProtocol.getType(),handlerMap.get(defaultProtocol.getType()),handlerMap,protocolHandlers); //修改：传协议类型
        channelDispatcher.injectWorkerLoop(workerLoop);
        Thread boss = new Thread(()->{
            try {
                bossLoop.loop();
            }catch (IOException ex){
                ex.printStackTrace();
            }
        },"boss-1");

        Thread worker = new Thread(()->{
            try{
                workerLoop.loop();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        },"worker-1");

        boss.start();
        worker.start();
    }
}
