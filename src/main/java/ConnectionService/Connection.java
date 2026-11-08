package ConnectionService;

import Core.EventLoop;

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
}
