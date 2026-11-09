package ConnectionService;

import Core.EventLoop;
import Protocol.Protocol;
import Protocol.Protocols;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.concurrent.ArrayBlockingQueue;

public class Connection {
    private EventLoop eventLoop;
    private ConnectionState state;
    private final int STATE_QUEUE_SIZE = 64;

    public Connection(EventLoop eventLoop,ConnectionState state){
        this.eventLoop = eventLoop;
        this.state = state;
    }
    public Connection(){

    }
    public boolean offerAndWeakUp(Object object){
        if(state.queue == null){
            synchronized (state){
                if(state.queue == null){
                    state.queue = new ArrayBlockingQueue<>(STATE_QUEUE_SIZE);
                }
            }
        }
        boolean success = state.queue.offer(object);
        if(!success) {
          return false;
        }
        eventLoop.selector.wakeup();
        return true;
    }

    public int getTotalByteSize(){
        ArrayBlockingQueue<Object> queue =  state.queue;
        ByteBuffer byteBuffer = state.writeBuffer;
        int size = 0;
        for(Object object : queue){
           int len = (Protocols)object.getLength();
           size += len;
        }

        //遍历队列
        //加上缓冲区大小
        size = queuebytesize + byteBuffer.size();
        //如果大于负载因子的话
        if(size >= Load * byteBuffer.capacity()){
            expand(byteBuffer);
        }
    }
    public ByteBuffer expand(ByteBuffer byteBuffer){
        if()

        return biggerBuffer;
    }

}
