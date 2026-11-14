package ConnectionService;

import Core.EventLoop;
import Core.WorkerLoop;
import Protocol.Protocol;
import java.nio.ByteBuffer;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;

public class Connection {
    private EventLoop eventLoop;
    private ConnectionState state;
    private final int STATE_QUEUE_SIZE = 64;
    private final int LOAD = 2;
    public Connection(EventLoop eventLoop,ConnectionState state){
        this.eventLoop = eventLoop;
        this.state = state;
    }
    public Connection(){

    }
    public boolean offerAndWeakUp(Protocol message) throws InterruptedException {
        if(state.queue == null){
            synchronized (state){
                if(state.queue == null){
                    state.queue = new ArrayBlockingQueue<>(STATE_QUEUE_SIZE);
                }
            }
        }
        if(!ensureWriteCapacity()){
            rejectHandler(1);
        }else{
            rejectHandler(2);
        }
        boolean success = state.queue.offer(message);
        if(!success) {
          return false;
        }
        eventLoop.selector.wakeup();
        return true;
    }
    public boolean ensureWriteCapacity(){
        ArrayBlockingQueue<Protocol> queue = state.queue;
        // 由所属 Worker 调用，避免和 Socket 写入同时修改 writeBuffer。
        ByteBuffer byteBuffer = state.writeBuffer;

        int size = byteBuffer == null ? 0 : byteBuffer.remaining();
        if (queue != null) {
            for (Protocol message : queue) {
                size += message.getLength();
            }
        }
        if (byteBuffer != null && eventLoop instanceof WorkerLoop worker) {
            while (size > (long) byteBuffer.capacity() * LOAD) {
                ByteBuffer biggerBuffer = worker.expand(byteBuffer, false); // 新增：扩容待发送的 buffer
                if (biggerBuffer == byteBuffer) {
                    return false;
                }
                state.writeBuffer = biggerBuffer;
                byteBuffer = biggerBuffer;
            }
        }

        return true;
    }
    public void rejectHandler(int signal){
        eventLoop.cancelInterest(signal,state);
    }
}
