package ConnectionService;

import Protocol.Protocol;
import ProtocolParser.ProtocolParser;

import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.util.Objects;
import java.util.concurrent.ArrayBlockingQueue;

public class ConnectionState {
    public SelectionKey key;
    public ProtocolParser protocolParser;
    public int writePending = 0;
    public volatile ArrayBlockingQueue<Protocol> queue;
    public ByteBuffer readBuffer = ByteBuffer.allocate(1024);
    public ByteBuffer writeBuffer = null;
}
