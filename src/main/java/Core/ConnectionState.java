package Core;

import java.nio.ByteBuffer;

public class ConnectionState {
    public ByteBuffer readBuffer = ByteBuffer.allocate(1024);
    public ByteBuffer writeBuffer;
}
