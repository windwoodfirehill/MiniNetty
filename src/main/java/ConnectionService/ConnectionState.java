package ConnectionService;

import Protocol.Protocol;

import java.nio.ByteBuffer;

public class ConnectionState {
    public Protocol protocol;
    public ByteBuffer readBuffer = ByteBuffer.allocate(1024);
    public ByteBuffer writeBuffer;

}
