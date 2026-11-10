package MiniNettyApplication;

import Core.BossLoop;
import Core.ChannelDispatcher;
import Core.WorkerLoop;
import Handler.MessageHandler;
import ProtocolParser.ProtocolParser;
import ProtocolParser.HttpProtocolParser;
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
    private final ProtocolParser defaultProtocolParser;
    private Map<String, Supplier<ProtocolParser>> protocolParsers;
    public MiniNettyBootStrap(int defaultPort,String defaultHost,ProtocolParser defaultProtocolParser){
        this.defaultPort = defaultPort;
        this.defaultHost = defaultHost;
        this.defaultProtocolParser = defaultProtocolParser;
    }
    public void setHandler(String protocolType,MessageHandler messageHandler){
        handlerMap.put(protocolType,messageHandler);
    }

    public void MiniteyStart() throws IOException {
        //协议获取映射表
        String[] protocols = {"http"};
        protocolParsers = Map.of(protocols[0],HttpProtocolParser :: new);
        //事件分发器
        ChannelDispatcher channelDispatcher = new ChannelDispatcher();
        //注册
        BossLoop bossLoop = new BossLoop(new InetSocketAddress(defaultHost, defaultPort),channelDispatcher); //修改
        WorkerLoop workerLoop = new WorkerLoop(defaultProtocolParser.getType(),
                handlerMap.get(defaultProtocolParser.getType()), handlerMap, protocolParsers);
        //注入事件分发器
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
