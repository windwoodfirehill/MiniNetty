package Core;

import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class ChannelDispatcher {
    private List<WorkerLoop> workerLoops;
    private AtomicInteger index;
    public ChannelDispatcher(){
        this.workerLoops = new ArrayList<>(16);
        this.index = new AtomicInteger(0);
    }
    public void injectWorkerLoop(WorkerLoop workerLoop){
        workerLoops.add(workerLoop);
    }
    public boolean submit(SocketChannel socketChannel){
        int size = workerLoops.size();
        if(size == 0){
            return false;
        }
        int selected = Math.floorMod(index.getAndIncrement(),size);
        return workerLoops.get(selected).offerToQueue(socketChannel);
    }

}

