package ConnectionService;

import Protocol.Protocol;

import java.nio.ByteBuffer;
import java.util.Objects;
import java.util.concurrent.ArrayBlockingQueue;

public class ConnectionState {
    public Protocol protocol;
    public volatile ArrayBlockingQueue<Object> queue;
    public ByteBuffer readBuffer = ByteBuffer.allocate(1024);
    public ByteBuffer writeBuffer = null;
}
