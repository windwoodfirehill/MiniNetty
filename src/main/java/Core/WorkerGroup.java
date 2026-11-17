package Core;

import Handler.MessageHandler;
import ProtocolParser.ProtocolParser;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

public class WorkerGroup {
    //
    private int MAX_SIZE = 10;
    public final Map<Integer,WorkerLoop> workerLoopMap = new LinkedHashMap<>();
    public ChannelDispatcher channelDispatcher;
    public boolean stop = false;

    public WorkerGroup(ChannelDispatcher channelDispatcher){
        this.channelDispatcher = channelDispatcher;
    }
    //此方法可以创建并返回worker参数先放空
    public boolean creatLoop(String protocolType, MessageHandler messageHandler, Map<String,MessageHandler> handlerMap,
                                   Map<String, Supplier<ProtocolParser>> protocolParsers,Map<Integer,WorkerLoop> temp,int idx)throws IOException{
        if(workerLoopMap.size() >= MAX_SIZE){
            return false;
        }
        if(stop){
            return  false;
        }
        //本身会抛出异常
        WorkerLoop workerLoop = null;
        workerLoop = new WorkerLoop(protocolType,
                handlerMap.get(protocolType), handlerMap, protocolParsers);
        temp.put(idx,workerLoop); //修改：成功前只写临时 Map
        return true;
    }

    public boolean creatWorkerLoop(String protocolType, MessageHandler messageHandler,Map<String,MessageHandler> handlerMap,
                                   Map<String, Supplier<ProtocolParser>> protocolParsers)throws IOException{
        //修改：单个创建复用批量流程
        return creatWorkerLoop(protocolType, messageHandler, handlerMap, protocolParsers, 1);
    }

    public synchronized boolean creatWorkerLoop(String protocolType, MessageHandler messageHandler,Map<String,MessageHandler> handlerMap,
                                  Map<String, Supplier<ProtocolParser>> protocolParsers,int number) throws IOException {

        if (number <= 0) {
            number = 1;
        }
        if (stop || MAX_SIZE - workerLoopMap.size() < number) {
            return false;
        }
        Map<Integer, WorkerLoop> temp = new LinkedHashMap<>();
        //修改：用最大已有 ID 分配新编号，避免删除 Worker 后撞号
        int nextId = workerLoopMap.keySet().stream().mapToInt(Integer::intValue).max().orElse(-1) + 1;
        try {
            for (int i = 0; i < number; i++) {
                if (!creatLoop(protocolType, messageHandler, handlerMap, protocolParsers, temp, nextId + i)) {
                    throw new IOException("Worker 创建中止"); //修改：整批回滚
                }
            }
            execute(temp);
        } catch (IOException | RuntimeException e) {
            closeAll(temp, e);
            throw e; //修改：保留原异常和清理异常
        }
        workerLoopMap.putAll(temp); //修改：整批启动后才登记和发布
        channelDispatcher.injectByMap(temp);
        return true;
    }
    public void closeAll(Map<Integer,WorkerLoop> temp,Throwable originalError) {
        for (Map.Entry<Integer, WorkerLoop> entry : temp.entrySet()) {
            try {
                WorkerLoop workerLoop = entry.getValue();
                workerLoop.shutdown(); //修改：若本批有线程已启动，先通知退出
                workerLoop.closeWorker();
            } catch (IOException closeError) {
                originalError.addSuppressed(closeError);
            }
        }
    }
    public void execute(Map<Integer,WorkerLoop> temp) throws IOException {
        for (Map.Entry<Integer, WorkerLoop> workerLoopEntry : temp.entrySet()) {
            Thread worker = new Thread(()->{
                try{
                    workerLoopEntry.getValue().loop();
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            },"worker-"+workerLoopEntry.getKey());
            worker.start(); //修改：补分号
        }
    }

}
