package Core;

import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;


public class
ChannelDispatcher {
    private volatile List<WorkerLoop> workerLoops;
    private final AtomicInteger index;
    public ChannelDispatcher(){
        this.workerLoops = List.of();
        this.index = new AtomicInteger(0);
    }
    public synchronized void injectWorkerLoop(WorkerLoop workerLoop){
        appendWorkers(List.of(workerLoop)); //修改：单个 Worker 也通过快照发布
    }
    public boolean submit(SocketChannel socketChannel){
        List<WorkerLoop> snapshot = workerLoops; //修改：本次轮询使用同一份名单
        int size = snapshot.size();
        if(size == 0){
            return false;
        }
        int selected = Math.floorMod(index.getAndIncrement(),size);
        return snapshot.get(selected).offerToQueue(socketChannel);
    }
    public synchronized void injectByMap(Map<Integer,WorkerLoop> temp){
        appendWorkers(temp.values()); //修改：整批一次发布
    }
    //新增：在更新锁内建立新名单，Boss 仍可读取旧快照
    private void appendWorkers(java.util.Collection<WorkerLoop> workers) {
        List<WorkerLoop> updated = new ArrayList<>(workerLoops);
        updated.addAll(workers);
        workerLoops = List.copyOf(updated);
    }

}

